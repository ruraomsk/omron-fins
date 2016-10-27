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
import ru.list.ruraomsk.fwlib.FwBaseMess;
import ru.list.ruraomsk.fwlib.FwKvit;
import ru.list.ruraomsk.fwlib.FwMesCtrl;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSetup;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;
import ru.list.ruraomsk.fwlib.FwUtil;
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
    private JTextArea Texts;
    private JScrollPane jScrollPan1;
    private JScrollPane jScrollPan2;
    private JScrollPane jScrollPan3;
    private JPanel pan;
    private String UPCMessage = "My system is code=0";

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
        Texts = new JTextArea();
        Texts.setColumns(2);
        Texts.setRows(5);
        jScrollPan3 = new JScrollPane();
        jScrollPan3.setViewportView(Texts);
        pan = new JPanel(new BorderLayout());
        pan.add(jScrollPan1, BorderLayout.NORTH);
        pan.add(jScrollPan2, BorderLayout.SOUTH);
        pan.add(jScrollPan3, BorderLayout.CENTER);
        Monitor.tabbed.add(name, pan);
        start();
    }

    @Override
    public void run()
    {
        try {
            while (!Thread.interrupted()) {
                synchronized (Monitor.frame) {
                    Date dd = new Date();
                    Message.setText("Текущее состояние на " + dd.toString());
                    Message.append("\nТекущий номер посылки=" + Integer.toString(sd.nomerinfo)
                            + " Ошибок=" + Integer.toString(sd.error));
                    Message.append("\nТекущий номер посылки диагностики=" + Integer.toString(sd.nomerdiag));
                    Message.append("\nПоследняя синхронизация " + sd.lastsync.toString());
                    UPCMessage = "My system is code=" + Integer.toString(sd.nomerdiag);
                    sd.setUPCMessage(UPCMessage);
                    Message.append("\nСостояние UPC=" + UPCMessage);
                    Message.append("\nПоследняя синхронизация " + sd.lastsync.toString());

                    Message.append("\nПодключено мастеров " + sd.listners.size());
                    for (SlaveListner sl : sd.listners.values()) {
                        Message.append("\nСоединение=" + (sl.isconnected() ? "Есть" : "Отсутствует")
                                + " Счетчик жизни=" + Integer.toString(sl.LiveCount()));
                    }
                    FwBaseMess mess;
                    while ((mess = sd.readMessage()) != null) {
                        if (mess.getFunctionCode() == FwUtil.FP_CODE_34H) {
                            Texts.append("\nПоступила команда на установку  " + dd.toString());
                            FwSetup ms = (FwSetup) mess;
                            Texts.append("\nКоманда=" + Integer.toString(ms.getCmd())
                                    + " №=" + Integer.toString(ms.getNomer())
                                    + " № файла=" + Integer.toString(ms.getNomFile())
                                    + " Длина=" + Integer.toString(ms.getLenght()));
                            Texts.append("\nСообщение=[" + ms.getBuffer() + "]");
                            FwKvit back = new FwKvit(controller);
                            back.setCmd(ms.getCmd());
                            back.setNomFile(ms.getNomFile());
                            back.setNomer(ms.getNomer());
                            Texts.append("\nОтправлена квитанция");
                            sd.putMessage(back);
                            if(ms.getCmd()==FwUtil.FW_CMD_RECIVE){
                                FwSetup recive=new FwSetup(false, controller, ms.getNomFile(), ms.getNomer(),FwUtil.FW_CMD_LAST);
                                recive.setData("abcd".getBytes(), 4);
                                sd.putMessage(recive);
                            Texts.append("\nОтправлен ответный буфер");
                            }
                        }
                        if (mess.getFunctionCode() == FwUtil.FP_CODE_10H) {
                            Texts.append("\nПоступила управляющая команда  " + dd.toString());
                            FwMesCtrl ms = (FwMesCtrl) mess;
                            Texts.append("\nКоманда=" + Integer.toString(ms.getCommandcode()));
                            for (Integer i = 0; i < 6; i++) {
                                Texts.append(" P" + i.toString() + "=" + Integer.toString(ms.getPar(i)));
                            }

                        }

                    }

                }
                Thread.sleep(sd.getStepTime());
            }
        }
        catch (InterruptedException ex) {
            Monitor.appendMessage("Устройство " + name + " завершает работу");
        }
    }

}
