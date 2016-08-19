/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwTransport extends Thread
{

    private DataInputStream m_Input;
    private DataOutputStream m_Output;

    private boolean connect = false;
    private FwRegisters tableDecode = null;
    private ConcurrentLinkedQueue<FwResponse> qResp = null;
    private Socket socket = null;
    public int error=0;
    public String textError="Not Error";

    public FwTransport(Socket socket, FwRegisters tableDecode)
    {
        this.socket = socket;
        this.tableDecode = tableDecode;
        qResp = new ConcurrentLinkedQueue();
    }

    public boolean connect()
    {

        try
        {
            connect=false;
            
            m_Input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            m_Output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            qResp.clear();
            start();
            connect = true;
            return connect;
        }
        catch (IOException ex)
        {
            if(FwUtil.FP_DEBUG) System.err.println("FwTransposrt connect "+ex.getMessage());
            error=1;
            close();
        }
        return connect;
    }

    public void close()
    {
        connect = false;
        try
        {
            m_Input.close();
            m_Output.close();
            socket.close();
        }
        catch (IOException ex)
        {
        }
    }

    public boolean isConnected()
    {
        return connect;
    }

    public FwResponse readMessage()
    {
        return qResp.poll();
    }

    /**
     * Передача сообщения
     *
     * @param msg
     * @throws IOException
     */
    public void writeMessage(FwMessage msg) 
    {
        try
        {
            if (!isConnected()) return;
            synchronized (FwUtil.outbuf){
                msg.writeTo(m_Output);
            }
            m_Output.flush();
        }
        catch (IOException ex)
        {
            if(FwUtil.FP_DEBUG) System.err.println("WriteMessage "+ex.getMessage());
            error=2;
        }
    }

    @Override
    public void run()
    {
        byte[] inb = new byte[5];
        while (!Thread.interrupted() && isConnected())
        {
            try
            {
                //textError="Читаем из потока пять байт";
                if (m_Input.read(inb, 0, 5) != 5)
                {
                    if(FwUtil.FP_DEBUG) System.err.println("Bad header");
                    error=3;
                    close();
                    //m_Input.skip(m_Input.available());
                    continue;
                }
                synchronized (FwUtil.buffer)
                {
                    Integer len = FwUtil.ToShort(inb, 0);
                    Integer controller = FwUtil.ToShort(inb, 2);
                    byte functionCode = inb[4];
                    //System.out.println("buffer len " +len.toString()+" "+controller.toString()+" "+Integer.toHexString(functionCode));
                    if ((len < FwUtil.MIN_LEN) || (len > FwUtil.MAX_LEN))
                    {
                        if(FwUtil.FP_DEBUG) System.err.println("Bad lenght");
                        error=4;
                        m_Input.skip(m_Input.available());
                        continue;                    //throw new IOException("Error from lenght.");
                    }
                    //textError="Читаем из потока байт"+Integer.toString(len);
                    if (m_Input.read(FwUtil.buffer, 0, len) == len)
                    {
                        //textError="Читаем из потока два байта";
                        if (m_Input.read(inb, 0, 2) != 2)
                        {
                            if(FwUtil.FP_DEBUG) System.err.println("Bad reading CRC " + Integer.toHexString(functionCode));
                            error=5;
                            continue;
                        }
                        int crc = FwUtil.ToShort(inb, 0);
                        if (crc == FwUtil.Crc(FwUtil.buffer, 0, len))
                        {
                            //textError="Разбираем ответ "+Integer.toHexString(functionCode);
                            qResp.offer(new FwResponse(controller, functionCode, len, FwUtil.buffer, tableDecode));
                            //textError="Разобрали ответ "+Integer.toHexString(functionCode);
                            
                        } else
                        {
                            error=6;
                            if(FwUtil.FP_DEBUG) System.err.println("Bad CRC " + Integer.toString(crc) + " wait= " + Integer.toString(FwUtil.Crc(FwUtil.buffer, 0, len)));
                        }
                    } else
                    {
                        error=7;
                        if(FwUtil.FP_DEBUG) System.out.println("Not correct len");
                    }
                }
            }
            catch (Exception ex)
            {
                //textError=ex.getMessage();
                if(FwUtil.FP_DEBUG) System.out.println("FwTransport "+ex.getMessage());
                error=8;
                close();
            }
        }
    }

    /**
     * @return the tableDecode
     */
    public FwRegisters getTableDecode()
    {
        return tableDecode;
    }
}
