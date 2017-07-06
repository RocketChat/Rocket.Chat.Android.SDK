package com.github.rocketchat.model;

import java.util.ArrayList;

import io.rocketchat.livechat.model.DepartmentObject;

/**
 * Created by sachin on 3/7/17.
 */

public class Department  {
    DepartmentObject object;

    Department(DepartmentObject object){
        this.object=object;
    }

    @Override
    public String toString() {
        return object.getDeptName();
    }

    public static ArrayList<Department> getDepartments(ArrayList <DepartmentObject> departmentObjects){
        ArrayList <Department> departments=new ArrayList<>();
        for (DepartmentObject object: departmentObjects){
            if (object.getShowOnRegistration()){
                departments.add(new Department(object));
            }
        }
        return departments;
    }

    public String getId(){
        return object.getId();
    }
}
