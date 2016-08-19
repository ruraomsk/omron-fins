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
 * @author Юрий
 */
public class ListnerServer extends Thread
{

    int port;
    FwRegisters tableDecode = null;
    Device devslave = null;

    ListnerServer(int port, FwRegisters tableDecode)
    {
        this.port = port;
        this.tableDecode = tableDecode;
        start();
    }

    public Device getDevice()
    {
        return devslave;
    }

    @Override
    public void run()
    {
        ServerSocket slave =null;
        int counter = 2000;
        try
        {
            slave = new ServerSocket(this.port);
            System.out.println("Slave waiting");
            Socket insc = slave.accept();
            System.out.println("Slave gogo!!");
            devslave = new Device("slave", insc, tableDecode, 500L);
            while (!Thread.interrupted())
            {
                System.out.println("Slave!");
                FwResponse resp =null ;
                while ((resp=devslave.getMessage()) != null)
                {
                    FwMesLive mesin = resp.getMesLive();

                    if (mesin != null)
                    {
                        System.out.println("Slave read Count=" + Integer.toString(mesin.getCounter()));
                    }
                }
                Thread.sleep(1000L);
                counter=(++counter>20000)?0:counter;
                FwMessage mess = new FwMessage( new FwMesLive(0,counter));
                devslave.addMessaqe(mess);
                System.out.println("Slave write Count=" + Integer.toString(counter));
            }
        }

        catch (IOException ex)
        {
            System.out.println("Error server " + ex.toString());
        }
        catch (InterruptedException ex)
        {
        }
        try
        {
            slave.close();
        }
        catch (IOException ex)
        {
        }
    }
}
