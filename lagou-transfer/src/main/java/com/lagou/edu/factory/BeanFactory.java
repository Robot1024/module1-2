package com.lagou.edu.factory;

import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyService;
import com.lagou.edu.annotation.MyTransactional;
import com.lagou.edu.dao.impl.JdbcAccountDaoImpl;
import com.lagou.edu.service.TransferService;
import com.lagou.edu.utils.ClassScaner;
import com.lagou.edu.utils.ClassUtils;
import com.lagou.edu.utils.ConnectionUtils;
import com.sun.jdi.InterfaceType;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象

    // 最好的实现是容器交给web 容器去创建
    static{
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            Element basePackage = (Element) rootElement.selectSingleNode("//component-scan");


            String basePackageName = basePackage.attributeValue("base-package");

            Set<Class> classes = ClassScaner.scan(basePackageName,MyService.class);


            // 实例化Bean
            for (Class cs:classes){

                MyService myService = (MyService) cs.getAnnotation(MyService.class);
                if("".equals(myService.value()) || myService.value()==null){

                    // 存储到map中待用
//                    String split[] = cs.getName().split("\\.");
//                    String className = split[split.length-1];
                    map.put(cs.getName(),cs.newInstance());
                }else {
                    map.put(myService.value(),cs.newInstance());
                }

            }


            // 注入Bean 的getBeansOfType依赖
            for (Class cs:classes){
                Field[] fields = cs.getDeclaredFields();
                for (int i=0;i<fields.length;i++){
                    MyAutowired myAutowired = (MyAutowired)fields[i].getAnnotation(MyAutowired.class);
                    if(myAutowired != null){
                        // 如果 requiredId 为空，按field 类型去装配，否则按照id 去装配
                        if("".equals(myAutowired.requiredId()) || myAutowired.requiredId()==null){

                            Class fieldClass = fields[i].getType();
                            // 判断一个 field 是不是一个接口类型,如果是接口类型，进入if。查找所有接口实现类
                            if(fieldClass.isInterface()){

                                List<Class> list = ClassUtils.getAllClassByInterface(fieldClass);

                                for(Class classInterface:list){
                                    if(null != map.get(classInterface.getName())){
                                        Object autowiredClass = map.get(classInterface.getName());
                                        fields[i].setAccessible(true);
                                        fields[i].set(map.get(cs.getName()),autowiredClass);
                                    }
                                }

                            }else {
                                String className = fields[i].getType().getName();
                                Object autowiredClass = map.get(className);
                                fields[i].setAccessible(true);

                                fields[i].set(map.get(cs.getName()),autowiredClass);
                            }

                        }else {
                            Object autowiredClass = map.get(myAutowired.requiredId());
                            fields[i].setAccessible(true);

                            fields[i].set(map.get(cs.getName()),autowiredClass);
                        }
                    }

                }

            }

            System.out.println("生成事务的动态代理对象！！");


            for (Class cs:classes) {
                Method[] methods = cs.getMethods();
                for (int i=0;i<methods.length;i++){
                    MyTransactional myAutowired = (MyTransactional)methods[i].getAnnotation(MyTransactional.class);

                    if(null != myAutowired){
                        // 带接口方法，可以用jdk 动态代理，否则用cglib
                        ProxyFactory proxyFactory = (ProxyFactory)getBean("com.lagou.edu.factory.ProxyFactory");
                        if(map.get(cs.getName()).getClass().getInterfaces().length > 0){
                            // 将代理对象，在放入map 中
                            map.put(cs.getName(),proxyFactory.getJdkProxy(map.get(cs.getName())));

                        }else {
                            // 将代理对象，在放入map 中
                            map.put(cs.getName(),proxyFactory.getCglibProxy(map.get(cs.getName())));

                        }

                    }

                }
            }


        }catch (DocumentException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

//        ProxyFactory proxyFactory = (ProxyFactory)getBean("com.lagou.edu.factory.ProxyFactory");
//        TransferService transferService = (TransferService) proxyFactory.getJdkProxy(getBean("com.lagou.edu.service.impl.TransferServiceImpl")) ;

    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

    public static void main(String[] args) {
        System.out.println("开始执行");
    }



}
