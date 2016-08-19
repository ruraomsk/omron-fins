/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import static java.awt.EventQueue.invokeLater;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import ru.list.ruraomsk.fwlib.FwUtil;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class Monitor
{

    static public HashMap<Long, String> globalName = null;
    static public JTextArea Message;
    static JFrame frame;
    static JTabbedPane tabbed;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException
    {
        // TODO code application logic here
        FwUtil.FP_DEBUG=true;
        JScrollPane jScrollPan2;
        JPanel pan;
        Message = new JTextArea();
        Message.setColumns(1);
        Message.setRows(10);
        Message.setEditable(false);
        jScrollPan2 = new JScrollPane();
        jScrollPan2.setViewportView(Message);
        pan = new JPanel(new BorderLayout());
        pan.add(jScrollPan2, BorderLayout.SOUTH);
        tabbed=new JTabbedPane();
        pan.add(tabbed,BorderLayout.NORTH);
        frame = new JFrame("Монитор устройств");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 1000));
        frame.add(pan);
        frame.setLocation(0,0);
        frame.pack();
        invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                frame.setVisible(true);
            }
        });
        Loader loader = new Loader();
    }

    public static void appendMessage(String message)
    {
        Date date = new Date();
        Monitor.Message.append(date.toString() + "\t" + message + "\n");
    }
}
