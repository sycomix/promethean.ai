package ai.promethean.API;
import ai.promethean.ExecutingAgent.*;
import ai.promethean.Parser.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.promethean.DataModel.*;
import ai.promethean.Planner.*;


/**
 * This API is meant to be as simple as possible and expandable as possible.
 * To add new functionality define your own function and the arguments it takes.
 * ParserError and PlannerError classes serve to throw descriptive errors from specific components of the system.
 * Example, if we want to add a function to create resource objects, simply create that function and then
 * call it with the resource object that keeps track of the examples
 */

public class API {
    public API(){

    }
    public void throwPlannerError(String err_msg){
        throw new PlannerError(err_msg);
    }

    public void throwParserError(String err_msg){
        throw new ParserError(err_msg);
    }

    public ArrayList<Object> parseInput(String inputFile, Boolean isFile){
        Parser p = new Parser(inputFile, isFile);
        ArrayList<Object> objects = p.parse();
        return objects;
    }

    /**
     * Generate a plan to be used by planner from list of parsed objects
     * @param parsedObjects List of objects generated by parser
     */
    public Plan generatePlanFromParsedObjects(ArrayList<Object> parsedObjects){
        Algorithm algo = new AStar((SystemState) parsedObjects.get(1),
                (GoalState) parsedObjects.get(2),
                (TaskDictionary) parsedObjects.get(3),
                (StaticOptimizations) parsedObjects.get(0));
        Planner planner = new Planner(algo);
        Plan plan = planner.plan();
        return plan;
    }

    public Plan generatePlanFromSystemState(SystemState currentState, GoalState goalState, TaskDictionary taskDictionary, StaticOptimizations optimizations){
        Algorithm algo = new AStar(currentState, goalState, taskDictionary, optimizations);
        Planner planner = new Planner(algo);
        Plan plan = planner.plan();
        return plan;
    }
    /**
     * Parse the input file and generate a plan from the parsed objects.
     * Initialize the clock and handle perturbation or goal state responses
     */
    public void executePlan(String inputFile, Boolean isFile){
        boolean planCompleted = false;

        ArrayList<Object> objects = parseInput(inputFile, isFile);
        Plan plan = generatePlanFromParsedObjects(objects);

        //Pull out the goalState, taskDict, and optimizations incase we need to replan
        GoalState goalState = (GoalState) objects.get(2);
        TaskDictionary taskDict = (TaskDictionary) objects.get(3);
        StaticOptimizations optimizations = (StaticOptimizations) objects.get(0);

        System.out.println("\nInitial State:\n======================");
        System.out.println(plan.getInitialState());
        System.out.println("\nRuntime Goal State:\n======================");
        System.out.println(plan.getGoalState());
        System.out.println("\nPlan:\n======================");
        while (!planCompleted){
            ClockObserver.addState(plan.getInitialState());
            Clock clock = new Clock(plan.getInitialState().getTime());
            ClockObserver tasks = new TaskExecutor(plan);
            clock.addObserver(tasks);
            //TODO: Should refactor parser objects to look up dictionary instead of checking size
            if (objects.size() >= 5) {
                ClockObserver perturbations = new PerturbationInjector((List<Perturbation>)objects.get(4));
                clock.addObserver(perturbations);
            }
            clock.runClock();
            planCompleted = ((TaskExecutor)tasks).isPlanCompleted();
            System.out.println(planCompleted);
            if(planCompleted){
                //TODO: Make this call the goal state handler
                System.out.println("Plan completed");
            }
            // a perturbation has occurred and needs to be handled.
            else{
                //get the current state of the craft for replanning
                System.out.println("===================== Replanning =====================");
                SystemState currentState = ClockObserver.peekLastState();
                plan = generatePlanFromSystemState(currentState, goalState, taskDict, optimizations);
                ArrayList<PlanBlock> list = plan.getPlanBlockList();
             /*   for (PlanBlock block: list) {
                    System.out.println(block.getTask());
                    System.out.println("\n");
                    System.out.println(block.getState());
                    System.out.println("\n");
                }*/
            }
        }
    }
}

