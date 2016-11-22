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
    public String textError = "Not Error";
    private Integer[] count=new Integer[5];
    private int cnt=0;
    public FwTransport(Socket socket, FwRegisters tableDecode)
    {
        this.socket = socket;
        this.tableDecode = tableDecode;
        qResp = new ConcurrentLinkedQueue();
        for (int i = 0; i < count.length; i++) {
            count[i]=0;
            
        }
    }

    public boolean connect()
    {

        try {
            connect = false;

            m_Input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            m_Output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            qResp.clear();
            start();
            connect = true;
            return connect;
        }
        catch (IOException ex) {
                System.err.println("FwTransposrt connect " + ex.getMessage());
            close();
        }
        return connect;
    }

    public void close()
    {
        connect = false;
        try {
            if (m_Input != null) {
                m_Input.close();
            }
            if (m_Output != null) {
                m_Output.close();
            }
            if (socket != null) {
                socket.close();
            }
        }
        catch (IOException ex) {
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
        try {
            if (!isConnected()) {
                return;
            }
            synchronized (FwUtil.outbuf) {
                msg.writeTo(m_Output);
            }
            m_Output.flush();
        }
        catch (IOException ex) {
                System.err.println("WriteMessage " + ex.getMessage());
        }
    }

    @Override
    public void run()
    {
        byte[] inb = new byte[5];
        while (!Thread.interrupted() && isConnected()) {
            try {
                //textError="Читаем из потока пять байт";
                if (m_Input.read(inb, 0, 5) != 5) {
                        System.err.println("Bad header");
                    close();
                    //m_Input.skip(m_Input.available());
                    continue;
                }
                synchronized (FwUtil.buffer) {
                    System.arraycopy(inb, 0, FwUtil.crcbuffer, 0, 5);
                    Integer len = FwUtil.ToShort(inb, 0);
                    Integer controller = FwUtil.ToShort(inb, 2);
                    byte functionCode = inb[4];
//                    System.err.println("buffer len " +len.toString()+" "+controller.toString()+" "+Integer.toHexString(functionCode));
                    if ((len < FwUtil.MIN_LEN) || (len > FwUtil.MAX_LEN)) {
                        errormessage(1);    
//                        System.err.println("Bad lenght");
                        m_Input.skip(m_Input.available());
                        continue;                    //throw new IOException("Error from lenght.");
                    }
                    //textError="Читаем из потока байт"+Integer.toString(len);
                    if (m_Input.read(FwUtil.buffer, 0, len) == len) {
                        //textError="Читаем из потока два байта";
                        if (m_Input.read(inb, 0, 2) != 2) {
                        errormessage(2);    

//                            System.err.println("Bad reading CRC " + Integer.toHexString(functionCode));
                            continue;
                        }
                        System.arraycopy(FwUtil.buffer, 0, FwUtil.crcbuffer, 5, len);
                        int crc = FwUtil.ToShort(inb, 0) & 0x7FFF;
                        if (crc == FwUtil.Crc(FwUtil.crcbuffer, 0, len + 5)) {
                            //textError="Разбираем ответ "+Integer.toHexString(functionCode);
                            errormessage(0);
                            qResp.offer(new FwResponse(controller, functionCode, len, FwUtil.buffer, tableDecode));
                            //textError="Разобрали ответ "+Integer.toHexString(functionCode);
                        }
                        else {
                            errormessage(3);
//                                System.err.println("Bad CRC " + Integer.toString(crc) + " wait= " + Integer.toString(FwUtil.Crc(FwUtil.crcbuffer, 0, len + 5)));
                                m_Input.skip(m_Input.available());
                        }
                    }
                    else {
                        errormessage(4);
//                            System.err.println("Not correct len");
                            m_Input.skip(m_Input.available());
                    }
                }
            }
            catch (Exception ex) {
                //textError=ex.getMessage();
                    System.err.println("FwTransport " + ex.getMessage());
                close();
            }
        }
    }
private void errormessage(int pos){
    count[pos]+=1;
    cnt++;
    if(cnt>5000){
        String result="report ";
        for (int i = 0; i < count.length; i++) {
            result+=count[i].toString()+" ";
            count[i]=0;
        }
        System.err.println(result);
        cnt=0;
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
