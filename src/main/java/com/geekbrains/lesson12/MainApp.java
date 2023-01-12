package com.geekbrains.lesson12;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.LockModeType;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class MainApp {
    public static void main(String[] args) {
        try (SessionFactory sessionFactory = new Configuration()
                .configure("hibernate.cfg.xml")
                .addAnnotatedClass(Item.class)
                .buildSessionFactory()) {
            for (int i = 0; i < 40; i++) {
                Session session = sessionFactory.getCurrentSession();
                session.beginTransaction();
                session.save(new Item());
                session.getTransaction().commit();
            }
//            CountDownLatch countDownLatch = new CountDownLatch(8); ЗАЧЕМ ЭТО ДЕЛАЕТСЯ??
            long time = System.currentTimeMillis();
            for (int i = 0; i < 8; i++) {
                new Thread(() ->
                {
                    for (int j = 0; j < 20_000; j++) {
                        Session session1 = sessionFactory.getCurrentSession();
                        session1.beginTransaction();
                        Long randomItem = (long) new Random().nextInt(40) + 1;
                        Item item = session1.createQuery("FROM Item i WHERE i.id = :id", Item.class)
                                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                .setParameter("id", randomItem)
                                .getSingleResult();
                        item.setVal(item.getVal() + 1);
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        session1.getTransaction().commit();
                        session1.close();
                        System.out.println(Thread.currentThread().getName() + " отработал");
                    }
//                    countDownLatch.countDown(); ЗАЧЕМ ЭТОТ СЧЕТЧИК ЗДЕСЬ?
                }).start();
            }
//            ПОЧЕМУ JOIN здесь работает?
//            try {
//                Thread.currentThread().join();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
            Session session = sessionFactory.getCurrentSession();
            System.out.println("time: " + (System.currentTimeMillis() - time));
            Long sum = (Long) session.createNativeQuery("SELECT  SUM(val) FROM items;").getSingleResult();
            System.out.println("!!!!!!!!!!!!!!!" + sum + "!!!!!!!!!!!!!!!");
        }

        System.out.println("main closed");
    }

    // Хотел в отдельную функцию, но что-то пошло не так
//    public static void parallelIncreaseValByOneManyTimes(SessionFactory sessionFactory) {
//
//    }
}