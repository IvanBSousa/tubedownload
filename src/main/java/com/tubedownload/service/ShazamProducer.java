//package com.tubedownload.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.shazamapi.audio.AudioNormalizer;
//import io.shazamapi.shazam.ShazamClient;
//import io.shazamapi.shazam.ShazamConfig;
//import io.shazamapi.shazam.ShazamService;
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.enterprise.inject.Produces;
//import jakarta.inject.Named;
//
//import java.lang.reflect.Field;
//
//@ApplicationScoped
//public class ShazamProducer {
//
//    @Produces
//    @ApplicationScoped
//    @Named("shazamService")
//    public ShazamService createShazamService() {
//        ShazamService service = new ShazamService();
//        ShazamClient client = new ShazamClient();
//        setField(client, "objectMapper", new ObjectMapper());
//        setField(client, "config", defaultConfig());
//        setField(service, "audioNormalizer", new AudioNormalizer());
//        setField(service, "shazamClient", client);
//        return service;
//    }
//
//    private static ShazamConfig defaultConfig() {
//        return new ShazamConfig() {
//            @Override
//            public String lang() {
//                return "pt-BR";
//            }
//
//            @Override
//            public String region() {
//                return "BR";
//            }
//
//            @Override
//            public String timezone() {
//                return "America/Sao_Paulo";
//            }
//        };
//    }
//
//    private static void setField(Object target, String fieldName, Object value) {
//        try {
//            Field field = target.getClass().getDeclaredField(fieldName);
//            field.setAccessible(true);
//            field.set(target, value);
//        } catch (ReflectiveOperationException e) {
//            throw new IllegalStateException("Failed to initialize " + target.getClass().getSimpleName() + "." + fieldName, e);
//        }
//    }
//}
