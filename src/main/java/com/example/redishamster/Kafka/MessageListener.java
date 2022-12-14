package com.example.redishamster.Kafka;

import Model.JsonHamsterItem;
import Model.JsonHamsterOrder;
import Model.JsonHamsterUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CacheConfig(cacheNames = "hc")
public class MessageListener {


    @Autowired
    private MongoTemplate mt;
    @Autowired
    private MessageProducer mp;

    @Autowired
    private UserProducer userKafkaTemplate;


    @KafkaListener(topics = "SaveHamster", containerFactory = "kafkaListenerContainerFactory")
    public void SaveHamster(String hamster){
        if (!mt.exists(Query.query(Criteria.where("_id").is(Integer.parseInt(findId(hamster)))), hamster)) {
            mt.insert(new JsonHamsterItem(Integer.parseInt(findId(hamster)), hamster));
        }
        else System.out.println("Duplicated Id! Check if "+ Integer.parseInt(findId(hamster)) +" is correct");
    }
    @KafkaListener(topics = "GetHamster", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterItem", key="#id")
    public void GetHamster(String id){
        int jsonId = Integer.parseInt(id);
        JsonHamsterItem jhi = mt.findById(jsonId, JsonHamsterItem.class);
        assert jhi != null;
        mp.sendMessage("SendHamster", jhi.getItemJson());
    }
    @KafkaListener(topics = "GetAllHamsters", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterItem")
    public void GetAllHamsters(){
        List<JsonHamsterItem> list= mt.findAll(JsonHamsterItem.class);
        StringBuilder message = new StringBuilder();
        for (JsonHamsterItem jsonHamsterItem : list) {
            message.append(jsonHamsterItem.getItemJson());
        }
        mp.sendMessage("SendHamster", message.toString());
        System.out.println(message);
    }
    @KafkaListener(topics = "SaveHamsters", containerFactory = "kafkaListenerContainerFactory")
    public void SaveHamsters(String hamsters){

        Pattern p = Pattern.compile("\\W\\s+\\\"id\\\"");
        String[] splitted = p.split(hamsters);
        Matcher m = p.matcher(hamsters);
        m.find();
        for (int i = 1; i<splitted.length; i++){
            splitted[i] = m.group() + splitted[i];
            if (!mt.exists(Query.query(Criteria.where("_id").is(Integer.parseInt(findId(splitted[i])))), splitted[i])) {
                System.out.println("Consumed item with id " + Integer.parseInt(findId(splitted[i])));
                mt.insert(new JsonHamsterItem(Integer.parseInt(findId(splitted[i])), splitted[i]));
            }
            else System.out.println("Duplicated Id! Check if "+ Integer.parseInt(findId(splitted[i])) +" is correct");
        }
    }
    @KafkaListener(topics = "DeleteHamster", containerFactory = "kafkaListenerContainerFactory")
    @CacheEvict(value="JsonHamsterItem", key="#id")
    public void DeleteHamster(String id){
        mt.findAllAndRemove(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), JsonHamsterItem.class);
    }

    @KafkaListener(topics = "UpdateHamster", containerFactory = "kafkaListenerContainerFactory")
    @CachePut(value="Hamster", key="#id")
    public void UpdateHamster(String id, String hamster){
        mt.findAndReplace(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), hamster);
    }
    @KafkaListener(topics = "SaveOrder", containerFactory = "kafkaListenerContainerFactory")
    public void SaveOrder(String order){
        if (!mt.exists(Query.query(Criteria.where("_id").is(Integer.parseInt(findId(order)))), order)){
            mt.insert(new JsonHamsterOrder(Integer.parseInt(findId(order)), order));
        }
        else System.out.println("Duplicated Id! Check if "+ Integer.parseInt(findId(order)) +" is correct");
    }
    @KafkaListener(topics = "SaveOrders", containerFactory = "kafkaListenerContainerFactory")
    public void SaveOrders(String orders){

        Pattern p = Pattern.compile("\\W\\s+\\\"id\\\"");
        String[] splitted = p.split(orders);
        Matcher m = p.matcher(orders);
        m.find();
        for (int i = 1; i<splitted.length; i++){
            splitted[i] = m.group() + splitted[i];
            if (!mt.exists(Query.query(Criteria.where("_id").is(Integer.parseInt(findId(splitted[i])))), splitted[i])) {
                mt.insert(new JsonHamsterOrder(Integer.parseInt(findId(splitted[i])), splitted[i]));
            }
            else System.out.println("Duplicated Id! Check if "+ Integer.parseInt(findId(splitted[i])) +" is correct");
        }
    }
    @KafkaListener(topics = "GetOrder", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterOrder", key="#id")
    public void GetOrder(String id){
        int jsonId = Integer.parseInt(id);
        JsonHamsterOrder jho = mt.findById(jsonId, JsonHamsterOrder.class);
        assert jho != null;
        mp.sendMessage("SendOrder", jho.getOrderItems());
    }
    @KafkaListener(topics = "GetAllOrders", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterOrder")
    public void GetAllOrders(){
        List<JsonHamsterOrder> list= mt.findAll(JsonHamsterOrder.class);
        StringBuilder message = new StringBuilder();
        for (JsonHamsterOrder jsonHamsterOrder : list) {
            message.append(jsonHamsterOrder.getOrderItems());
        }
        mp.sendMessage("SendHamster", message.toString());
        System.out.println(message);
    }
    @KafkaListener(topics = "DeleteOrder", containerFactory = "kafkaListenerContainerFactory")
    @CacheEvict(value="JsonHamsterOrder", key="#id")
    public void DeleteOrder(String id){
        mt.findAndRemove(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), JsonHamsterOrder.class);
    }
    @KafkaListener(topics = "UpdateOrder", containerFactory = "kafkaListenerContainerFactory")
    @CachePut(value="JsonHamsterOrder", key="#id")
    public void UpdateOrder(String id, String order){
        mt.findAndReplace(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), order);
    }
    @KafkaListener(topics = "SaveUser", containerFactory = "userKafkaListenerContainerFactory")
    public void SaveUser(JsonHamsterUser user){
        System.out.println(user);
        long userId = System.currentTimeMillis();
        if (mt.exists(Query.query(Criteria.where("_id").is(userId)), JsonHamsterUser.class)) {
            userId += System.currentTimeMillis();
        }
        user.setId(userId);
        mt.insert(user);
        System.out.println(mt.find(Query.query(Criteria.where("_id").is(userId)), JsonHamsterUser.class));
    }
//    @KafkaListener(topics = "SaveUsers", containerFactory = "userKafkaListenerContainerFactory")
//    public void SaveUsers(String users){
//
//        Pattern p = Pattern.compile("\\W\\s+\\\"id\\\"");
//        String[] splitted = p.split(users);
//        Matcher m = p.matcher(users);
//        m.find();
//        for (int i = 1; i<splitted.length; i++){
//            splitted[i] = m.group() + splitted[i];
//            if (!mt.exists(Query.query(Criteria.where("_id").is(Integer.parseInt(findId(splitted[i])))), splitted[i])) {
//                mt.insert(new JsonHamsterUser(Integer.parseInt(findId(splitted[i])), splitted[i]));
//            }
//            else System.out.println("Duplicated Id! Check if "+ Integer.parseInt(findId(splitted[i])) +" is correct");
//        }
//    }
    @KafkaListener(topics = "GetUser", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterUser", key="#id")
    public void GetUser(String username){
        JsonHamsterUser jhu = mt.findOne(Query.query(Criteria.where("username").is(username)), JsonHamsterUser.class);
        assert jhu != null;
        userKafkaTemplate.sendMessage("SendUser", jhu);
    }
    @KafkaListener(topics = "GetAllUsers", containerFactory = "kafkaListenerContainerFactory")
    @Cacheable(value="JsonHamsterUser")
    public void GetAllUsers(){
        List<JsonHamsterUser> list= mt.findAll(JsonHamsterUser.class);
        StringBuilder message = new StringBuilder();
        for (JsonHamsterUser jsonHamsterUser : list) {
            message.append(jsonHamsterUser);
        }
        mp.sendMessage("SendHamster", message.toString());
        System.out.println(message);
    }
    @KafkaListener(topics = "DeleteOrder", containerFactory = "kafkaListenerContainerFactory")
    @CacheEvict(value="JsonHamsterUser", key="#id")
    public void DeleteUser(String id){
        mt.findAndRemove(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), JsonHamsterUser.class);
    }
    @KafkaListener(topics = "UpdateOrder", containerFactory = "kafkaListenerContainerFactory")
    @CachePut(value="JsonHamsterUser", key="#id")
    public void UpdateUser(String id, String user){
        mt.findAndReplace(Query.query(Criteria.where("_id").is(Integer.parseInt(id))), user);
    }


    public String findId(String hamster){
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(hamster);
        if (m.find())
        {
            return m.group();
        }
        else {
            System.out.println("Json doesn't have an id");
            return null;
        }
    }
    public String findUsername(String user){
        Pattern p = Pattern.compile("(?<=username\\\"\\:\\s\\\").*(?=\\\",)");
        Matcher m = p.matcher(user);
        if (m.find())
        {
            return m.group();
        }
        else {
            System.out.println("Json doesn't contain an username");
            return null;
        }
    }
    public String findEmail(String user){
        Pattern p = Pattern.compile("(?<=\\\"email\\\"\\:\\s\\\").*(?=\\\",)");
        Matcher m = p.matcher(user);
        if (m.find())
        {
            return m.group();
        }
        else {
            System.out.println("Json doesn't contain an username");
            return null;
        }
    }
    public String findPassword(String user){
        Pattern p = Pattern.compile("(?<=\\\"password\\\"\\:\\s\\\").*(?=\\\")");
        Matcher m = p.matcher(user);
        if (m.find())
        {
            return m.group();
        }
        else {
            System.out.println("Json doesn't contain an username");
            return null;
        }
    }
}
