/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwSlave extends Thread
{

    private int port;
    private int count = 0;
    private String type = "SL:";

    public FwSlave(int port)
    {
        this.port = port;
        start();
    }

    @Override
    public void run()
    {
        ServerSocket slave = null;
        try {
            slave = new ServerSocket(this.port);
            while (!Thread.interrupted()) {
                Socket insc = slave.accept();
                String name = type + Integer.toString(port) + "-" + Integer.toString(count++);
                FwSlaveListner sdev = new FwSlaveListner(name, port, insc);
                sdev.startListner();
                if (FwUtil.FP_DEBUG) {
                    System.err.println("Запущен " + name);
                }
            }
        }
        catch (IOException ex) {
            if (FwUtil.FP_DEBUG) {
                System.err.println("Ошибка FwSlave " + ex.getMessage());
            }
        }
        finally {
            try {
                slave.close();
            }
            catch (IOException ex) {
            }
        }

    }

}
