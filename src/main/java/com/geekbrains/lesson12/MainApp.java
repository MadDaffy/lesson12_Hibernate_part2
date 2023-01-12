package com.geekbrains.lesson12;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.LockModeType;
import java.util.Random;
import java.util.concurrent.CountDownLatch;


//        1. Создайте таблицу items (id serial, val int, ...), добавьте в нее 40 строк со значением 0;
//        2. Запустите 8 параллельных потоков, в каждом из которых работает цикл, выбирающий
//        случайную строку в таблице и увеличивающий val этой строки на 1. Внутри транзакции
//        необходимо сделать Thread.sleep(5). Каждый поток должен сделать по 20.000 таких
//        изменений;
//        3. По завершению работы всех потоков проверить, что сумма всех val равна соответственно
//        160.000;

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
            CountDownLatch countDownLatch = new CountDownLatch(8);
            Thread[] threads = new Thread[8];
            long time = System.currentTimeMillis();
            for (int i = 0; i < threads.length; i++) {
               threads[i] = new Thread(() ->
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
                    countDownLatch.countDown();
                });
               threads[i].start();
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Session session = sessionFactory.getCurrentSession();
            System.out.println("time: " + (System.currentTimeMillis() - time));
            Long sum = (Long) session.createNativeQuery("SELECT  SUM(val) FROM items;").getSingleResult();
            System.out.println("!!!!!!!!!!!!!!!" + sum + "!!!!!!!!!!!!!!!");
        }

        System.out.println("main closed");
    }

    // Необходимо вынести большинство логики в отдельную функцию
//    public static void parallelIncreaseValByOneManyTimes(SessionFactory sessionFactory) {
//
//    }
}