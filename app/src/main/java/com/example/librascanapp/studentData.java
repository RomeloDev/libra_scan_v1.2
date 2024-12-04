package com.example.librascanapp;

public class studentData {
    private String id, name, courseYr, department;

    //default Constructor
    public studentData(){

    }

    public studentData(String id, String name, String courseYr, String department){
        this.id = id;
        this.name = name;
        this.courseYr = courseYr;
        this.department = department;
    }

    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getCourseYr(){
        return courseYr;
    }

    public String getDepartment(){
        return department;
    }

}
