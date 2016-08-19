/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author Юрий
 */
public class TestDevices
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException
    {
        int port=5022;
        FwRegisters tableDecode = new FwRegisters();

        ListnerServer serv = new ListnerServer(port, tableDecode);
        Socket master = new Socket(InetAddress.getLocalHost(), port);
        Device t1 = new Device("one", master, tableDecode, 500L);
        int counter=0;
        while (!Thread.interrupted())
        {
            System.out.println("Master!");
            counter=(++counter>30000)?0:counter;
            FwMessage mess = new FwMessage( new FwMesLive(0,counter));
            t1.addMessaqe(mess);
            System.out.println("Master write Count=" + Integer.toString(counter));
            
            Thread.sleep(1000L);
            FwResponse resp = null;
            while ((resp = t1.getMessage())!=null)
            {
                    FwMesLive mesin = resp.getMesLive();
                    if (mesin != null)
                    {
                        System.out.println("Master read Count=" + Integer.toString(mesin.getCounter()));
                    }
            }

        }
        
    }

}
