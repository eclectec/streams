package com.streams;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

import com.google.gson.*;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.StreamEntry;

import redis.clients.jedis.UnifiedJedis;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;



public class Main {
    public static Gson gson = new Gson();
    public static String broker = System.getenv().getOrDefault("BROKER", "redis");
    public static int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "6379"));
    public static String topic = System.getenv().getOrDefault("TOPIC", "rumble");
    
    public static void main(String[] args) {
        try{
            // Make sure Redis is spun up
            TimeUnit.SECONDS.sleep(20);
            runRedisStreams();
        }  catch (Exception e) {
            e.printStackTrace();
        }
        // if(System.getenv("BROKER") == "redis") {
        //     runRedisStreams();
        // } else {
        //     runKafkaStreams();
        // }
    }

    public static void runRedisStreams() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(poolConfig, broker, port);

        Jedis jpub = jedisPool.getResource();
        
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    if(message != null && message.length() > 0) {
                        String geoData = getGeoObject(message);

                        jpub.publish(topic, geoData);
                    }
                }
            }, "traffic");
        }
    }


    // public static void runKafkaStreams() {
    //     Properties props = new Properties();
    //     props.put(StreamsConfig.APPLICATION_ID_CONFIG, "rumble");
    //     props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9093");
    //     props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    //     props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

    //     StreamsBuilder builder = new StreamsBuilder();
    //     KStream<String, String> source = builder.stream("traffic");

    //     source.map((key, message) -> {
    //         String geoData = getGeoObject(message);

    //         return KeyValue.pair(key, geoData);
    //     }).to(topic);;

    //     KafkaStreams streams = new KafkaStreams(builder.build(), props);
    //     streams.start();
    // }

    private static String getGeoObject(String message) {
        JsonObject aircraft = gson.fromJson(message, JsonObject.class);
        JsonObject geoJsonObj = new JsonObject();
        geoJsonObj.addProperty("type", "Feature");
                    
        JsonObject geoObj = new JsonObject();
        geoObj.addProperty("type", "Point");
        
        JsonArray coords = new JsonArray();
        coords.add(aircraft.get("lon"));
        coords.add(aircraft.get("lat"));
        geoObj.add("coordinates", coords);	
        geoJsonObj.add("geometry", geoObj);
        
        JsonObject propertiesObject = aircraft;
        String icao = aircraft.has("hex") ? aircraft.get("hex").toString().replaceAll("\"", "").trim() : aircraft.get("icao").toString().replaceAll("\"", "").trim();
        propertiesObject.addProperty("icao", icao);
        String uniqueId = String.format("rumble-%s",icao);
        propertiesObject.addProperty("uid", uniqueId);

        geoJsonObj.add("properties", propertiesObject);

        return gson.toJson(geoJsonObj);
    }
}
