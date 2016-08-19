/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Тестируем функции библиотеки. Типа серверная часть
 *
 * @author Юрий
 */
public class Device extends Thread
{

    private FwTransport tr = null;
    private ConcurrentLinkedQueue<FwResponse> qResp = null;
    private ConcurrentLinkedQueue<FwMessage> qMess = null;
    private FwRegisters tableDecode = null;
    private long sleepTime;
    private boolean error = false;
    private String name;

    Device(String name,Socket socket, FwRegisters tableDecode, long sleepTime) throws IOException
    {
        this.name=name;
        this.tableDecode = tableDecode;
        this.sleepTime = sleepTime;
        tr = new FwTransport(socket, tableDecode);
        qResp = new ConcurrentLinkedQueue();
        qMess = new ConcurrentLinkedQueue();
        System.out.println("Запустили "+name);
        start();
    }

/**
 * Добавить соощение в очередь вывода
 * @param mess
 * @return 
 */
    public boolean addMessaqe(FwMessage mess)
    {
        return qMess.offer(mess);
    }
/**
 * прочитать сообщение из очереди
 * @return 
 */
    public FwResponse getMessage()
    {
        return qResp.poll();
    }

    @Override
    public void run()
    {
        FwResponse resp = null;
        FwMessage mess = null;
        try
        {
            do
            {
                
                while ((resp = tr.readMessage()) != null)
                {
                   //System.out.println("Есть прочитанное сообщение в "+name);
                    qResp.add(resp);
                };
                while ((mess = qMess.poll()) != null)
                {
                    //System.out.println("Есть отправленное сообщение "+name);
                    tr.writeMessage(mess);
                }
                Thread.sleep(sleepTime);
            }
            while (!Thread.interrupted());
        }
        catch (InterruptedException ex)
        {
        }
        finally{
            tr.close();
        }
    }

    /**
     * @return the error
     */
    public boolean isError()
    {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(boolean error)
    {
        this.error = error;
    }

}
