package com.efimchick.ifmo;


import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Collecting {

    public int sum(IntStream intStream) {
        return intStream.reduce(Integer::sum).orElse(0);
    }

    public int production(IntStream intStream) {
        return intStream.reduce((x,y)->x*y).orElseGet(()->0);
    }

    public int oddSum(IntStream intStream) {
        return this.sum(intStream.filter(x->Math.abs(x)%2==1));
    }

    public Map<Integer, Integer> sumByRemainder(int divider, IntStream intStream) {

        Map<Integer,Integer> resMap = new TreeMap<>();

        intStream.forEach( x -> {
                    int key = x%divider;
                    resMap.computeIfPresent(key,(y,z)->resMap.get(key)+x);
                    resMap.putIfAbsent(key,x);
                });
        return resMap;
    }

    public Map<Person, Double> totalScores(Stream<CourseResult> results) {
        Map<Person,Double> resMap = new LinkedHashMap<>();
        results.forEach( x->{
            Map<String, Integer> current = x.getTaskResults();
            boolean toHistory = current.keySet().stream().noneMatch(y->y.contains("Lab"));
            double average = (double)(current.values()
                    .stream().reduce(0,Integer::sum)) / (toHistory ? 4 : 3);
            resMap.put(x.getPerson(),average);
            }
        );
        return resMap;
    }

    public double averageTotalScore(Stream<CourseResult> results) {

        final double[] res = {0,0};
        results.forEach( x->{
                    Map<String, Integer> current = x.getTaskResults();
                    boolean toHistory = current.keySet().stream().noneMatch(y->y.contains("Lab"));
                    double average = (double)(current.values().stream()
                            .reduce(0,Integer::sum)) / (toHistory ? 4 : 3);
                    res[0]++;
                    res[1]+=average;
                }
        );
        return res[1]/res[0];
    }

    public Map<String, Double> averageScoresPerTask(Stream<CourseResult> results) {

        Map<String,Double> returnMap = new HashMap<>();
        results.map(CourseResult::getTaskResults).forEach( x -> {

                    Set<String> lessons = x.keySet();
                    lessons.forEach( y-> {
                        returnMap.computeIfPresent(y,(z,c) ->(returnMap.get(y)+x.get(y)));
                        returnMap.putIfAbsent(y,(double)x.get(y));
                    });
        });
        returnMap.entrySet().forEach(x-> x.setValue(x.getValue()/3.0));
        return returnMap;
    }

    public Map<Person, String> defineMarks(Stream<CourseResult> results) {

        Map<Person,Double> marks = this.totalScores(results);
        Map<Person, String> resMap = new HashMap<>();
        marks.forEach( (per,mark)-> resMap.put(per,doubleToStringMark(mark)));
        return resMap;
    }

    private String doubleToStringMark(double mark){
        if (mark>90&&mark<=100) return "A";
        else if (mark>=83&&mark<=90) return "B";
        else if (mark>=75&&mark<83) return "C";
        else if (mark>=68&&mark<75) return "D";
        else if (mark>=60&&mark<68) return "E";
        else return "F";
    }


    public String easiestTask(Stream<CourseResult> results) {
        return this.averageScoresPerTask(results).entrySet().stream()
                        .max(Map.Entry.comparingByValue()).get().getKey();
    }

    public Collector<CourseResult, ?, String> printableStringCollector() {

            return Collectors.collectingAndThen(
                    Collectors.toList(),
                    courseResults -> {

                        //Персона з найдовшим ПІБ
                        Person maxWidthPerson = courseResults.stream().max((o1, o2)->{
                            Person p1 = o1.getPerson();
                            Person p2 = o2.getPerson();
                            return Integer.compare(p1.getFirstName().length()+p1.getLastName().length(),
                                            p2.getFirstName().length()+p2.getLastName().length());
                        }).get().getPerson();

                        //Ширина для 1 колонки імен
                        int width = maxWidthPerson.getFirstName().length() + maxWidthPerson.getLastName().length() ;
                        //Історія чи програмування
                        boolean toHistory = courseResults.get(0).getTaskResults().keySet().stream().noneMatch(y->y.contains("Lab"));

                        StringBuilder header = new StringBuilder();
                        header.append("Student");
                        header.append(" ".repeat(width - "Student".length()));
                        if (toHistory) header.append("  | Phalanxing | Shieldwalling | Tercioing | Wedging | Total | Mark |");
                        else header.append("  | Lab 1. Figures | Lab 2. War and Peace | Lab 3. File Tree | Total | Mark |");
                        //Шапка наче готова

                        courseResults.sort(Comparator.comparing(o -> o.getPerson().getLastName()));

                        StringJoiner result = new StringJoiner(System.lineSeparator());
                        result.add(header.toString());
                        double averageResFooter = 0;

                        for (CourseResult courseResult : courseResults) {

                            StringJoiner sj = new StringJoiner(" | ");
                            Person person = courseResult.getPerson();
                            String name = person.getFirstName();
                            String surname = person.getLastName();
                            sj.add(surname + " " + name+" ".repeat(width-surname.length()-name.length()));

                            Map<String, Integer> taskResults = courseResult.getTaskResults();
                            String key;
                            if (toHistory) addingValueToTable(sj, taskResults, "Phalanxing", "Shieldwalling", "Tercioing", "Wedging");
                            else addingValueToTable(sj, taskResults, "Lab 1. Figures", "Lab 2. War and Peace", "Lab 3. File Tree");

                            int total = taskResults.values().stream().mapToInt(Integer::intValue).sum();

                            double mark = (double)total / (taskResults.size() + (toHistory ? 1: 0));
                            averageResFooter+=mark;


                            sj.add(String.format("%.2f", mark).replace(",","."));
                            sj.add(String.format("   %s |", doubleToStringMark(mark)));
                            result.add(sj.toString());
                        }

                        StringBuilder footer = new StringBuilder();
                        footer.append("Average ").append(" ".repeat(width - "Average".length())).append(" | ");
                        StringJoiner sj = new StringJoiner(" | ");

                        Map<String,Double> averageValuesForLesson = new HashMap<>();
                        courseResults.stream().map(CourseResult::getTaskResults).forEach(x->{
                            x.keySet().forEach( keyStr-> {
                                averageValuesForLesson.computeIfPresent(keyStr,(z,c)->averageValuesForLesson.get(keyStr)+x.get(keyStr));
                                averageValuesForLesson.putIfAbsent(keyStr, (double)x.get(keyStr));
                            });
                        });

                        averageValuesForLesson.forEach((x,y)->averageValuesForLesson.put(x,y/3));
                        String format = "%.2f";
                        if (toHistory) addingValueToFooter(sj,averageValuesForLesson,"Phalanxing","Shieldwalling","Tercioing","Wedging");
                        else addingValueToFooter(sj,averageValuesForLesson,"Lab 1. Figures", "Lab 2. War and Peace", "Lab 3. File Tree");

                        sj.add(String.format(format,averageResFooter/3).replace(",","."));
                        sj.add(String.format("   %s |", doubleToStringMark(averageResFooter/3)));

                        footer.append(sj);
                        result.add(footer);
                        return result.toString().replace("\r\n","\n");
                    }
            );
    }

    private void addingValueToTable(StringJoiner sj, Map<String,Integer> taskResults, String... keys){

        for (String key: keys) {
            Integer value = taskResults.getOrDefault(key, 0);
            String valueStr = value.toString();
            sj.add(" ".repeat(key.length()-2) + " ".repeat(2-valueStr.length()) + valueStr);
        }
    }

    private void addingValueToFooter(StringJoiner sj, Map<String,Double> averageValuesForLesson, String... keys){
        String format = "%.2f";
        for (String key: keys) {
            Double value = averageValuesForLesson.getOrDefault(key, 0.0);
            sj.add(" ".repeat(key.length()-5) + String.format(format,value).replace(",","."));
        }
    }
}

