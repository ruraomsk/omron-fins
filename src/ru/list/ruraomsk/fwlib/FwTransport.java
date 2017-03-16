/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import com.tibbo.aggregate.common.Log;
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
public class FwTransport extends Thread {

    private DataInputStream m_Input;
    private DataOutputStream m_Output;

    private boolean connect = false;
    private FwRegisters tableDecode = null;
    private ConcurrentLinkedQueue<FwResponse> qResp = null;
    private Socket socket = null;
    private final Integer[] count = new Integer[5];
    private int cnt = 0;

    public FwTransport(Socket socket, FwRegisters tableDecode) {
        this.socket = socket;
        this.tableDecode = tableDecode;
        qResp = new ConcurrentLinkedQueue();
        for (int i = 0; i < count.length; i++) {
            count[i] = 0;

        }
    }

    public boolean connect() {

        try {
            connect = false;
            m_Input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            m_Output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            qResp.clear();
            start();
            connect = true;
            return connect;
        } catch (IOException ex) {
            Log.CORE.error("FwTransposrt connect " + ex.getMessage());
            close();
        }
        return connect;
    }

    public void close() {
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
        } catch (IOException ex) {
        }
    }

    public boolean isConnected() {
        return connect;
    }

    public FwResponse readMessage() {
        return qResp.poll();
    }

    /**
     * Передача сообщения
     *
     * @param msg
     * @throws IOException
     */
    public void writeMessage(FwMessage msg) {
        try {
            if (!isConnected()) {
                return;
            }
            msg.writeTo(m_Output);
            m_Output.flush();
        } catch (IOException ex) {
            Log.CORE.error("WriteMessage " + ex.getMessage());
        }
    }

    @Override
    public void run() {
        byte[] inb = new byte[5];
        while (!Thread.interrupted() && isConnected()) {
            try {
                //textError="Читаем из потока пять байт";
                Integer inbl;
                if ((inbl = m_Input.read(inb, 0, 5)) != 5) {
                    if (inbl < 0) {
                        Thread.sleep(250L);
                        continue;
                    }
                    Log.CORE.error("Bad header " + inbl.toString());
                    m_Input.skip(m_Input.available());
                    continue;
                }
                System.arraycopy(inb, 0, FwUtil.crcbuffer, 0, 5);
                Integer len = FwUtil.ToShort(inb, 0);
                Integer controller = FwUtil.ToShort(inb, 2);
                byte functionCode = inb[4];
                if ((len < FwUtil.MIN_LEN) || (len > FwUtil.MAX_LEN)) {
                    errormessage(1);
                    m_Input.skip(m_Input.available());
                    continue;                    //throw new IOException("Error from lenght.");
                }
                synchronized (FwUtil.buffer) {
                    if (m_Input.read(FwUtil.buffer, 0, len) == len) {
                        if (m_Input.read(inb, 0, 2) != 2) {
                            errormessage(2);
                            continue;
                        }
                        System.arraycopy(FwUtil.buffer, 0, FwUtil.crcbuffer, 5, len);
                        int crc = FwUtil.ToShort(inb, 0) & 0x7FFF;
                        if (crc == FwUtil.Crc(FwUtil.crcbuffer, 0, len + 5)) {
                            errormessage(0);
                            qResp.offer(new FwResponse(controller, functionCode, len, FwUtil.buffer, tableDecode));
                        } else {
                            errormessage(3);
                            m_Input.skip(m_Input.available());
                        }
                    } else {
                        errormessage(4);
                        m_Input.skip(m_Input.available());
                    }
                }
            } catch (Exception ex) {
                Log.CORE.error("FwTransport " + ex.getMessage());
                close();
            }
        }
    }

    private void errormessage(int pos) {
        count[pos] += 1;
        cnt++;
        if (cnt > 5000) {
            String result = "report ";
            for (int i = 0; i < count.length; i++) {
                result += count[i].toString() + " ";
                count[i] = 0;
            }
            Log.CORE.error(result);
            cnt = 0;
        }
    }

    /**
     * @return the tableDecode
     */
    public FwRegisters getTableDecode() {
        return tableDecode;
    }
}
