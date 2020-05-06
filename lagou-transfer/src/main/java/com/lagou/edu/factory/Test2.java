package com.lagou.edu.factory;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Test2 {

    public static void main(String[] args) throws Exception {
        //获得Dog类的Class对象
        Class<?> classType = Class.forName("com.lagou.edu.factory.Dog");
        //生成对象的实例
        Object obj = classType.newInstance();

        //取得dogName属性
        Field dogName = classType.getDeclaredField("dogName");
        //禁止Field的访问控制检查
        dogName.setAccessible(true);
        //将Field的值设为“Xiao Qiang”
        dogName.set(obj, "Xiao Qiang");

        //取得say()方法
        Method say = classType.getDeclaredMethod("say", new Class[]{});
        //禁止say方法的访问控制检查
        say.setAccessible(true);
        //调用say方法
        say.invoke(obj, new Object[]{});
    }

}
