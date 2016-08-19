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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import ru.list.ruraomsk.fwlib.FwMasterDevice;
import ru.list.ruraomsk.fwlib.FwOneReg;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;
import ru.list.ruraomsk.fwlib.SlaveListner;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
class MasterDisplay extends Thread
{

    private String name;
    private FwMasterDevice md;
    private int controller;
    private FwRegisters tableDecode;
    private JFrame jfrm;
    private JTable Info;
    private JTextArea Message;
    private JTextArea History;
    private JScrollPane jScrollPan1;
    private JScrollPane jScrollPan2;
    private JScrollPane jScrollPan3;
    private JPanel pan;

    MasterDisplay(String name, FwMasterDevice md)
    {
        this.name = name;
        this.md = md;
        controller = md.getController();
        tableDecode = md.getTableDecode();

        jScrollPan1 = new JScrollPane();
        Info = new JTable(new MsTable(tableDecode, md));
        jScrollPan2 = new JScrollPane();
        Message = new JTextArea();
        jScrollPan1.setViewportView(Info);
        Message.setColumns(1);
        Message.setRows(5);
        jScrollPan2.setViewportView(Message);
        History = new JTextArea();
        History.setColumns(45);
        History.setRows(40);
        jScrollPan3 = new JScrollPane();
        jScrollPan3.setViewportView(History);
        pan = new JPanel(new BorderLayout());
        pan.add(jScrollPan1, BorderLayout.WEST);
        pan.add(jScrollPan2, BorderLayout.SOUTH);
        pan.add(jScrollPan3, BorderLayout.EAST);
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
                    Message.append("\nТекущий номер посылки=" + Integer.toString(md.nomerinfo)
                            + " Ошибок=" + Integer.toString(md.error));
                    Message.append("\nПоследняя синхронизация " + md.lastsync.toString());
                    FwOneReg oreg = md.getHistory();
                    boolean need = false;
                    while (oreg != null)
                    {
                        History.append(oreg.toString() + "\n");
                        need = true;
                        oreg = md.getHistory();
                    }
                    if (need)
                    {
                        Info.updateUI();
                    }

                }
                Thread.sleep(md.getStepTime());

            }
        }
        catch (InterruptedException ex)
        {
            Monitor.appendMessage("Устройство " + name + " завершает работу");
        }
    }

}
