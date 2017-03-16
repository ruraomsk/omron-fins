/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextManager;
import com.tibbo.aggregate.common.datatable.DataTable;
import com.tibbo.aggregate.common.device.DisconnectionException;
import com.tibbo.aggregate.common.device.RemoteDeviceErrorException;
import com.tibbo.aggregate.common.protocol.RemoteServer;
import com.tibbo.aggregate.common.protocol.RemoteServerController;
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

    static public HashMap<Integer, String> globalName = null;
    static public JTextArea Message;
    static JFrame frame;
    static JTabbedPane tabbed;
    static PropertiesManager prop;
    static public DataTable registers;
    static public DataTable devices;
    static public String IPhost;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, DisconnectionException, RemoteDeviceErrorException, ContextException
    {
        // TODO code application logic here
        prop=new PropertiesManager();
        prop.loadProperties();
        IPhost=prop.getCurrent().getString("host");
        RemoteServer rls = new RemoteServer(IPhost, prop.getCurrent().getInt("port"),
                prop.getCurrent().getString("user"), prop.getCurrent().getString("password"));
        RemoteServerController rlc = new RemoteServerController(rls, true);
        rlc.connect();
        rlc.login();
        ContextManager cm = rlc.getContextManager();
        registers=cm.get(prop.getCurrent().getString("device")).getVariable("registers");
        devices=cm.get(prop.getCurrent().getString("device")).getVariable("devices");
        rlc.disconnect();
        
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
        tabbed = new JTabbedPane();
        pan.add(tabbed, BorderLayout.NORTH);
        frame = new JFrame("Имитатор устройств");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1900, 1000));
        frame.add(pan);
        frame.setLocation(10, 10);
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
