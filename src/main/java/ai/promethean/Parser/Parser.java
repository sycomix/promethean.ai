package ai.promethean.Parser;


import ai.promethean.DataModel.*;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Parser {
    private JsonParser parser = new JsonParser();
    private String json;
    //ArrayLists are mutable objects so we don't need a setter
    private ArrayList<Object> parsedObjects = new ArrayList<Object>();
    private TaskDictionary taskDictionary = new TaskDictionary();
    private StaticOptimizations optimizationList =  new StaticOptimizations();
    private ArrayList<Object> perturbationList =  new ArrayList<Object>();

    public Parser(){
        setJson("");
    }

    public Parser(String _json, Boolean isFile){
        if(!isFile) {
            setJson(_json);
        }
        else{
            try
            {
                String content = new String(Files.readAllBytes(Paths.get(_json)), StandardCharsets.ISO_8859_1);
               setJson(content);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    public void setJson(String _json){
        this.json=_json;
    }

    public ArrayList<Object> parse() {
        JsonElement jsonTree = parser.parse(json);
        JsonArray jsonArray = jsonTree.getAsJsonArray();
        if (jsonTree.isJsonArray()) {
            for(JsonElement j: jsonArray) {
                JsonObject jsonObject= j.getAsJsonObject();
                String title = jsonObject.get("title").getAsString();

                if (title.equalsIgnoreCase("Optimization")) {
                    String name = jsonObject.get("name").getAsString();
                    Boolean isMin = jsonObject.get("isMinimum").getAsBoolean();
                    Optimization o = new Optimization(name, isMin);
                    optimizationList.addOptimization(o);
                    //System.out.println(o);

                } else if (title.equalsIgnoreCase("State")) {
                    int UID = jsonObject.get("UID").getAsInt();
                    JsonElement time = jsonObject.get("time");
                    boolean isGoal= jsonObject.get("isGoal").getAsBoolean();
                    SystemState systemState;
                    if(time==null){
                         systemState= new SystemState(UID, isGoal);
                    }
                    else {
                        systemState = new SystemState(UID, time.getAsLong(), isGoal);
                    }
                    if(jsonObject.get("resources")!=null) {
                        JsonArray resources = jsonObject.get("resources").getAsJsonArray();
                        for (JsonElement elem : resources) {
                            JsonObject resource = elem.getAsJsonObject();
                            String name = resource.get("name").getAsString();
                            Double value = resource.get("value").getAsDouble();
                            systemState.addResource(name, value);
                        }
                    }

                    if(jsonObject.get("properties")!=null) {
                        JsonArray properties = jsonObject.get("properties").getAsJsonArray();
                        for (JsonElement elem : properties) {
                            JsonObject property = elem.getAsJsonObject();
                            String name = property.get("name").getAsString();
                            JsonPrimitive value = property.get("value").getAsJsonPrimitive();
                            if (value.getAsJsonPrimitive().isBoolean()) {
                                systemState.addProperty(name, value.getAsBoolean());
                            } else if (value.getAsJsonPrimitive().isNumber()) {
                                systemState.addProperty(name, value.getAsDouble());
                            } else if (value.getAsJsonPrimitive().isString()) {
                                systemState.addProperty(name, value.getAsString());
                            } else {
                                throw new IllegalArgumentException("Invalid property type");
                            }
                        }
                    }
                    parsedObjects.add(systemState);
                    //System.out.println(systemState);

                } else if (title.equalsIgnoreCase("Task")) {
                    int UID = jsonObject.get("UID").getAsInt();
                    int duration = jsonObject.get("duration").getAsInt();
                    Task task= new Task(UID,duration);

                    if(jsonObject.get("requirements")!=null) {
                        JsonArray requirements = jsonObject.get("requirements").getAsJsonArray();
                        for (JsonElement elem : requirements) {
                            JsonObject requirement = elem.getAsJsonObject();
                            String name = requirement.get("name").getAsString();
                            String operator = requirement.get("operator").getAsString();
                            JsonPrimitive value = requirement.get("value").getAsJsonPrimitive();
                            if (value.getAsJsonPrimitive().isBoolean()) {
                                task.addRequirement(name, value.getAsBoolean(), operator);
                            } else if (value.getAsJsonPrimitive().isNumber()) {
                                task.addRequirement(name, value.getAsDouble(), operator);
                            } else if (value.getAsJsonPrimitive().isString()) {
                                task.addRequirement(name, value.getAsString(), operator);
                            } else {
                                throw new IllegalArgumentException("Invalid requirement type");
                            }

                        }
                    }
                    if(jsonObject.get("resource_impacts")!=null) {
                        JsonArray resources = jsonObject.get("resource_impacts").getAsJsonArray();
                        for (JsonElement elem : resources) {
                            JsonObject resource = elem.getAsJsonObject();
                            String name = resource.get("name").getAsString();
                            Double value = resource.get("value").getAsDouble();
                            task.addResource(name, value);
                        }
                    }

                    if(jsonObject.get("property_impacts")!=null) {
                        JsonArray properties = jsonObject.get("property_impacts").getAsJsonArray();
                        for (JsonElement elem : properties) {
                            JsonObject property = elem.getAsJsonObject();
                            String name = property.get("name").getAsString();
                            JsonPrimitive value = property.get("value").getAsJsonPrimitive();
                            if (value.getAsJsonPrimitive().isBoolean()) {
                                task.addProperty(name, value.getAsBoolean());
                            } else if (value.getAsJsonPrimitive().isNumber()) {
                                task.addProperty(name, value.getAsDouble());
                            } else if (value.getAsJsonPrimitive().isString()) {
                                task.addProperty(name, value.getAsString());
                            } else {
                                throw new IllegalArgumentException("Invalid property type");
                            }
                        }
                    }
                    taskDictionary.addTask(task);
                    //System.out.println(task);

                } else if (title.equalsIgnoreCase("Perturbation")) {
                    long time = jsonObject.get("time").getAsLong();
                    Perturbation perturbation= new Perturbation(time);

                    //TODO resources and properties
                    JsonArray resources = jsonObject.get("resources").getAsJsonArray();
                    for(JsonElement elem: resources){
                        JsonObject resource= elem.getAsJsonObject();
                        String name= resource.get("name").getAsString();
                        Double value= resource.get("value").getAsDouble();
                        perturbation.addResource(name,value);
                    }

                    JsonArray properties= jsonObject.get("properties").getAsJsonArray();
                    for(JsonElement elem: properties){
                        JsonObject property= elem.getAsJsonObject();
                        String name= property.get("name").getAsString();
                        JsonPrimitive value= property.get("value").getAsJsonPrimitive();
                        if(value.getAsJsonPrimitive().isBoolean()){
                            perturbation.addProperty(name, value.getAsBoolean());
                        }
                        else if(value.getAsJsonPrimitive().isNumber()){
                            perturbation.addProperty(name, value.getAsDouble());
                        }
                        else if(value.getAsJsonPrimitive().isString()){
                            perturbation.addProperty(name,value.getAsString());
                        }
                        else{
                            throw new IllegalArgumentException("Invalid property type");
                        }
                    }
                    perturbationList.add(perturbation);
                    //System.out.println(perturbation);

                } else {
                    throw new IllegalArgumentException("JSON Object title does not exist");
                }
            }

        }
        parsedObjects.add(taskDictionary);
        parsedObjects.add(optimizationList);
        parsedObjects.add(perturbationList);
        return parsedObjects;
    }
}
