/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thesoftwareguild.wampclient;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.ApplicationError;
import ws.wamp.jawampa.Request;
import ws.wamp.jawampa.WampRouter;
import ws.wamp.jawampa.WampRouterBuilder;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;
import ws.wamp.jawampa.transport.netty.SimpleWampWebsocketListener;

/**
 *
 * @author apprentice
 */
public class Client {

    public static void main(String[] args) {
        new Client().start();
    }

    Subscription addProcSubscription;
    Subscription eventPublication;
    Subscription eventSubscription;
    String input;
    String name;
    Scanner sc = new Scanner(System.in);

    public void start() {
        IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
        WampClientBuilder builder = new WampClientBuilder();

        System.out.println("Enter your name:");
        name = sc.nextLine();

        final WampClient client;

        try {
            builder.withConnectorProvider(connectorProvider)
                    .withUri("ws://127.0.0.1:8080/ws1")
                    .withRealm("realm1")
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS);
            client = builder.build();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        client.statusChanged().subscribe(new Action1<WampClient.State>() {
            @Override
            public void call(WampClient.State t1) {
                //System.out.println("Session status changed to " + t1);

                if (t1 instanceof WampClient.ConnectedState) {
                    eventPublication = Schedulers.immediate().createWorker().schedule(new Action0() {
                        @Override
                        public void call() {
                            client.publish("com.myapp.test", name + " has connected.");
                        }
                    });

                    eventSubscription = client.makeSubscription("com.myapp.test", String.class)
                            .subscribe(new Action1<String>() {
                                @Override
                                public void call(String t1) {
                                    System.out.println(t1);
                                }

                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable t1) {
                                    System.out.println("There as an error: " + t1);
                                }
                            }, new Action0() {
                                @Override
                                public void call() {
                                    System.out.println("Message sent.");
                                }
                            });

                }
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                System.out.println("Session ended with error " + t);
            }
        }, new Action0() {
            @Override
            public void call() {
                System.out.println("Session ended.");
            }
        });

        client.open();

        boolean running = true;

        while (running) {
            input = sc.nextLine();
            if (input.equals("end program")) {
                running = false;
                eventPublication = Schedulers.immediate().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        client.publish("com.myapp.test", name + " has disconnected");
                    }
                });

                System.out.println("Closing client.");

                if (eventSubscription != null) {
                    eventSubscription.unsubscribe();
                }
                eventPublication.unsubscribe();
                client.close().toBlocking().last();
            } else {
                eventPublication = Schedulers.immediate().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        client.publish("com.myapp.test", name + ": " + input);
                    }
                });
            }

        }

    }
}
