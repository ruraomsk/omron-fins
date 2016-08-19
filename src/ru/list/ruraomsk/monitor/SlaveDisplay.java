/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import static java.awt.EventQueue.invokeLater;
import java.util.Date;
import javax.swing.*;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;
import ru.list.ruraomsk.fwlib.SlaveListner;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class SlaveDisplay extends Thread
{

    private String name;
    private FwSlaveDevice sd;
    private int controller;
    private FwRegisters tableDecode;
    private JFrame jfrm;
    private JTable Info;
    private JTextArea Message;
    private JScrollPane jScrollPan1;
    private JScrollPane jScrollPan2;
    private JPanel pan;

    SlaveDisplay(String name, FwSlaveDevice sd)
    {

        this.name = name;
        this.sd = sd;
        controller = sd.getController();
        tableDecode = sd.getTableDecode();

        jScrollPan1 = new JScrollPane();
        Info = new JTable(new SlTable(tableDecode, sd));
        jScrollPan2 = new JScrollPane();
        Message = new JTextArea();
        jScrollPan1.setViewportView(Info);
        Message.setColumns(2);
        Message.setRows(5);
        jScrollPan2.setViewportView(Message);
        pan = new JPanel(new BorderLayout());
        pan.add(jScrollPan1, BorderLayout.NORTH);
        pan.add(jScrollPan2, BorderLayout.SOUTH);
        Monitor.tabbed.add(name, pan);
        start();
    }

    @Override
    public void run()
    {
        try
        {
            while (!Thread.interrupted())
            {
                synchronized (Monitor.frame)
                {
                    Date dd = new Date();
                    Message.setText("Текущее состояние на " + dd.toString());
                    Message.append("\nТекущий номер посылки=" + Integer.toString(sd.nomerinfo)
                            + " Ошибок=" + Integer.toString(sd.error));
                    Message.append("\nТекущий номер посылки диагностики=" + Integer.toString(sd.nomerdiag));
                    Message.append("\nПоследняя синхронизация " + sd.lastsync.toString());
                    Message.append("\nПодключено мастеров " + sd.listners.size());
                    for (SlaveListner sl : sd.listners.values())
                    {
                        Message.append("\nСоединение=" + (sl.isconnected() ? "Есть" : "Отсутствует")
                                + " Счетчик жизни=" + Integer.toString(sl.LiveCount()));
                    }

                }
                Thread.sleep(sd.getStepTime());
            }
        }
        catch (InterruptedException ex)
        {
            Monitor.appendMessage("Устройство " + name + " завершает работу");
        }
    }

}
