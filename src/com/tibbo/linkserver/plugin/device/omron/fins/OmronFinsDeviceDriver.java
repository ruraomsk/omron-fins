/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tibbo.linkserver.plugin.device.omron.fins;

import com.tibbo.aggregate.common.Log;
import com.tibbo.aggregate.common.context.CallerController;
import com.tibbo.aggregate.common.context.ContextException;
import com.tibbo.aggregate.common.context.ContextUtils;
import com.tibbo.aggregate.common.context.EventDefinition;
import com.tibbo.aggregate.common.context.FunctionDefinition;
import com.tibbo.aggregate.common.context.VariableDefinition;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.validator.LimitsValidator;
import com.tibbo.aggregate.common.datatable.validator.ValidatorHelper;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.security.ServerPermissionChecker;
import com.tibbo.aggregate.common.util.AggreGateThread;
import com.tibbo.aggregate.common.util.ThreadManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ru.list.ruraomsk.fwlib.*;
import ruraomsk.list.ru.strongsql.DescrValue;
import ruraomsk.list.ru.strongsql.ParamSQL;
import ruraomsk.list.ru.strongsql.SetValue;
import ruraomsk.list.ru.strongsql.StrongSql;
import ruraomsk.list.ru.vlrmanager.VLRDataTableManager;
import ruraomsk.list.ru.vlrmanager.VLRXMLManager;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class OmronFinsDeviceDriver extends AbstractDeviceDriver {

    public ConcurrentHashMap<String, FwOneReg> data = null;
    public ConcurrentHashMap<Integer, String> reversdata = null;

    private FwRegisters tableDecode = null;
    public ArrayList<CanalMaster> canals = null;
    public FwLoadFile ldF;
    private long stepSQL;
    Long lastWriteSQL = 0L;

    private ThreadManager thrManRec = new ThreadManager();
    private MultiReconect thReadRec = null;
    ParamSQL param = null;
    VLRXMLManager vlrManager = null;
    StrongSql sSql = null;
    StrongSql sqldata;
    StrongSql sqlseek;

    public OmronFinsDeviceDriver() {
        super("fwdevice", VFT_CONNECTION_PROPERTIES);
    }

    @Override
    public void setupDeviceContext(DeviceContext deviceContext)
            throws ContextException {
        super.setupDeviceContext(deviceContext);
//        deviceContext.setDeviceType("com.tibbo.linkserver.plugin.device.omron-fins");
        deviceContext.setDefaultSynchronizationPeriod(10000L);
        VariableDefinition vd = new VariableDefinition("connectionProperties", VFT_CONNECTION_PROPERTIES, true, true, "connectionProperties", ContextUtils.GROUP_ACCESS);
        vd.setIconId("var_connection");
        vd.setHelpId("ls_drivers_fwdevice");
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("registers", VFT_REGISTERS, true, true, "Регистры", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("devices", VFT_DEVICES, true, true, "Устройства", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);

        vd = new VariableDefinition("SQLProperties", VFT_SQL, true, true, "Сохранение дампа", ContextUtils.GROUP_ACCESS);
        vd.setWritePermissions(ServerPermissionChecker.getAdminPermissions());
        deviceContext.addVariableDefinition(vd);
        makeRegisters(deviceContext);

        deviceContext.setDeviceType("fwdevice");
        ldF = new FwLoadFile(deviceContext, this);
    }
    @Override
    public void accessSettingUpdated(String name) {
        if ( name.equalsIgnoreCase("devices")) {
            try {
                makeRegisters(getDeviceContext());
            } catch (ContextException ex) {
                Log.CORE.error("Ошибка редакции " + name + " " + ex.getMessage());
            }
        }
        super.accessSettingUpdated(name); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void connect() throws DeviceException {
        try {
            makeAllTables();
            canals = new ArrayList<>();
            DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
            DataTable devs = getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
            DataTable sqlprop = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController());
            param = DriverHelper.setParamData(sqlprop);
            stepSQL = sqlprop.rec().getLong("stepSQL");
            devs.sort("controller", true);
            int controller = -1;
            for (DataRecord dev : devs) {
                try {
                    if (controller != dev.getInt("controller")) {
                        controller = dev.getInt("controller");
                        canals.add(new CanalMaster(controller, tableDecode, cp.getLong("timeOut")));
                    }
                    CanalMaster cm = canals.get(canals.size() - 1);
                    if (!dev.getString("IPaddr").equalsIgnoreCase("slave")) {
                        Socket socket = new Socket(InetAddress.getByName(dev.getString("IPaddr")), dev.getInt("port"));
                        cm.addDevice(socket);
                    } else {
                        cm.setSlave(dev.getInt("port"));
                    }
                    canals.set(canals.size() - 1, cm);
                } catch (IOException ex) {
                    Log.CORE.error("Devices mount error " + ex.getMessage());
                }
            }
            for (CanalMaster cm : canals) {
                for (FwMasterDevice dev : cm.mdarray) {
                    try {
                        dev.setStepTime(cp.getLong("stepTime"));
                        dev.setStepGiveMe(cp.getLong("giveTime"));
                        dev.setStepLive(cp.getLong("liveTime"));
                        dev.gogo();
                        if (!dev.isConnected()) {
                            Log.CORE.error("Device is NOT connected " + dev.myAddress());
                        }
                    } catch (Exception ex) {
                        Log.CORE.error("Devices error " + ex.getMessage());
                    }
                }
            }
        } catch (ContextException ex) {
            Log.CORE.error("Devices error " + ex.getMessage());
        }

        if (thReadRec == null) {
            thReadRec = new MultiReconect(this, thrManRec);
        }

        try {
            DataRecord sqlrec = getDeviceContext().getVariable("SQLProperties", getDeviceContext().getCallerController()).rec();
            if (sqlrec.getBoolean("createDB")) {
                ArrayList<DescrValue> arraydesc = new ArrayList<>();
                DataTable registers = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
                for (DataRecord reg : registers) {
                    String name = reg.getString("name");
                    int key = FwRegister.makeKey(reg.getInt("canal"), reg.getInt("uId"), reg.getInt("type") == 0);
                    arraydesc.add(new DescrValue(name, key, reg.getInt("format")));
                }
                new StrongSql(param, arraydesc, 1, sqlrec.getLong("lenghtDB"), "SPA-PS DataTable");
            }
            sqldata = new StrongSql(param, stepSQL);
            sqlseek = new StrongSql(param);
        } catch (ContextException ex) {
            throw new DeviceException("Ошибки SQL " + ex.getMessage());
        }

        super.connect();
    }

    @Override
    public void disconnect() throws DeviceException {
        super.disconnect(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null) {
            return;
        }
        for (CanalMaster cm : canals) {
            for (FwMasterDevice dev : cm.mdarray) {
                dev.disconect();
            }
        }
        sqldata.disconnect();
        sqlseek.disconnect();
        tableDecode = null;
        canals = null;
    }

    @Override
    public void startSynchronization() throws DeviceException {
        super.startSynchronization(); //To change body of generated methods, choose Tools | Templates.
        if (canals == null) {
            return;
        }
        if ((System.currentTimeMillis() - lastWriteSQL) > stepSQL) {
            ArrayList<SetValue> arrayValues = new ArrayList<>();
            for (FwOneReg oreg : data.values()) {
                if (oreg.getGood() == FwUtil.FP_DATA_NOGOOD) {
                    continue;
                }
                Integer newID = oreg.getReg().getKey();
                Object value = oreg.getValue();
                arrayValues.add(new SetValue(newID, oreg.getDate(), value, oreg.getGood()));
            }

            for (CanalMaster cm : canals) {
                cm.changeCanal();
                FwOneReg oreg;
                while ((oreg = cm.getHistory()) != null) {
                    Integer newID = oreg.getReg().getKey();
                    Object value = oreg.getValue();
                    arrayValues.add(new SetValue(newID, oreg.getDate(), value, oreg.getGood()));
                    String name = reversdata.get(oreg.getReg().getKey());
                    data.put(name, oreg);
                }
                cm.clearDatas();
            }
            if (!arrayValues.isEmpty()) {
                sqldata.addValues(new Timestamp(System.currentTimeMillis()), arrayValues);
            }
            lastWriteSQL = System.currentTimeMillis();
        }

        DriverHelper.makeFireEvent(this);
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void finishSynchronization() throws DeviceException, DisconnectionException {
        ldF.regEventListner();

    }

    @Override
    public DataTable readVariableValue(VariableDefinition vd, CallerController caller) throws ContextException, DeviceException, DisconnectionException {
        FwOneReg oreg = data.get(vd.getName());
        DataTable res = new DataTable(vd.getFormat());
        if (oreg == null) {
            res.addRecord(0);
            return res;
        }
        res.addRecord(oreg.getValue());
        return res;
    }

    @Override
    public List<EventDefinition> readEventDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException {
        List<EventDefinition> ed = new LinkedList<>();
        TableFormat Format = new TableFormat(true);
        Format.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        Format.addField(FieldFormat.create("<IPaddr><S><D=IP address устройства>"));
        Format.addField(FieldFormat.create("<port><I><D=Номер порта>"));
        Format.addField(FieldFormat.create("<file><I><D=Номер файла>"));
        Format.addField(FieldFormat.create("<nomer><I><D=Номер посылки>"));
        Format.addField(FieldFormat.create("<command><I><D=Код команды>"));
        Format.addField(FieldFormat.create("<result><I><D=Код результата>"));
        ed.add(new EventDefinition("Event_35H", Format, "Квитирование", ContextUtils.GROUP_DEFAULT));
        Format = new TableFormat(true);
        Format.addField(FieldFormat.create("<Controller><I><D=Номер канала>"));
        Format.addField(FieldFormat.create("<IPaddr><S><D=IP address устройства>"));
        Format.addField(FieldFormat.create("<port><I><D=Номер порта>"));
        Format.addField(FieldFormat.create("<file><I><D=Номер файла>"));
        Format.addField(FieldFormat.create("<nomer><I><D=Номер посылки>"));
        Format.addField(FieldFormat.create("<command><I><D=Код команды>"));
        Format.addField(FieldFormat.create("<lenght><I><D=Длина буфера>"));
        Format.addField(FieldFormat.create("<buffer><S><D=Строка от контроллера>"));
        ed.add(new EventDefinition("Event_36H", Format, "Настройка", ContextUtils.GROUP_DEFAULT));
        return ed;
    }

    @Override
    public List<FunctionDefinition> readFunctionDefinitions(DeviceEntities entities) {
        return DriverHelper.makerFunctionDefinitions();
    }

    @Override
    public DataTable executeFunction(FunctionDefinition fd, CallerController caller, DataTable parameters) throws ContextException, DeviceException, DisconnectionException {
        return DriverHelper.executeFunction(this, fd, caller, parameters);
    }

    @Override
    public List<VariableDefinition> readVariableDefinitions(DeviceEntities entities) throws ContextException, DeviceException, DisconnectionException {
        List res = new LinkedList();
        makeAllTables();
        DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
        for (DataRecord reg : regs) {
            char type = ' ';
            switch (reg.getInt("format")) {
                case 0:
                    type = 'B';
                    break;
                case 1:
                    type = 'I';
                    break;
                case 2:
                    type = 'F';
                    break;
            }

            FieldFormat ff = FieldFormat.create(reg.getString("name"), type, reg.getString("description"));
            TableFormat tf = new TableFormat(1, 1, ff);
            tf.setUnresizable(true);
            VariableDefinition vd = new VariableDefinition(reg.getString("name"), tf, true, false, reg.getString("description"), "remote");
            res.add(vd);
        }
        return res;
    }
    private void makeRegisters(DeviceContext deviceContext) throws ContextException {
        Map<String,DataTable> loadsDataTables=new HashMap<>();
        param = DriverHelper.setParamVlr(deviceContext.getVariable("SQLProperties", getDeviceContext().getCallerController()));
        if (vlrManager != null) {
            vlrManager.close();
        }
        vlrManager = new VLRXMLManager(param);
        if (!vlrManager.connected) {
            Log.CORE.error("Отсутствует соединение " + param.toString());
        }
        DataTable devices = deviceContext.getVariable("devices", getDeviceContext().getCallerController());
        for (DataRecord crec : devices) {
            String name=crec.getString("idvlr");
            DataTable tempData = VLRDataTableManager.fromXML(vlrManager.getXML(name, 1));
            for(DataRecord rec:tempData){
                rec.setValue("canal", crec.getInt("controller"));
            }
            loadsDataTables.put(name, tempData);
        }
        if(!loadsDataTables.isEmpty()){
            DataTable registers=new DataTable(VFT_REGISTERS);
            for(DataTable tab:loadsDataTables.values()){
                for(DataRecord r:tab){
                    DataRecord rr=registers.addRecord();
                    rr.setValue("name", r.getString("name"));
                    rr.setValue("description", r.getString("description"));
                    rr.setValue("canal", r.getInt("canal"));
                    rr.setValue("type", r.getInt("type"));
                    rr.setValue("uId", r.getInt("uId"));
                    rr.setValue("uId", r.getInt("uId"));
                    rr.setValue("format", r.getInt("format"));
                    rr.setValue("lenght", r.getInt("lenght"));
                    
                }
            }
            deviceContext.setVariable("registers", getDeviceContext().getCallerController(),registers);
        }
    }

    private void makeAllTables() {
        if (tableDecode != null) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            DataTable regs = getDeviceContext().getVariable("registers", getDeviceContext().getCallerController());
            tableDecode = new FwRegisters(FwUtil.VALUE_UIDS);
            data = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            reversdata = new ConcurrentHashMap<>(FwUtil.VALUE_UIDS);
            for (DataRecord reg : regs) {
                FwRegister fwreg = new FwRegister(reg.getInt("canal"), reg.getInt("type") == 0, reg.getInt("uId"), reg.getInt("format"), reg.getInt("lenght"));
                tableDecode.add(fwreg);
                FwOneReg oreg = new FwOneReg(now, fwreg);
                data.put(reg.getString("name"), oreg);
                reversdata.put(oreg.getReg().getKey(), reg.getString("name"));
            }
        } catch (ContextException ex) {
            Log.CORE.error("Registers not found" + ex.getMessage());
        }
    }

    private static final TableFormat VFT_CONNECTION_PROPERTIES;
    private static final TableFormat VFT_REGISTERS;
    private static final TableFormat VFT_DEVICES;
    private static final TableFormat VFT_SQL;

    private static Map TypeSelectionValues() {
        Map types = new LinkedHashMap();
        types.put(0, "Информация");
        types.put(1, "Диагностика");
        return types;
    }

    private static Map FormatSelectionValues() {
        Map reg = new LinkedHashMap();
        reg.put(0, "Логический");
        reg.put(1, "2-байтный Int Signed");
        reg.put(2, "4-байтный Float");
        return reg;
    }

    private static Map LenghtSelectionValues() {
        Map reg = new LinkedHashMap();
        reg.put(1, "Один байт");
        reg.put(2, "Два байта");
        reg.put(4, "Четыре байта");
        return reg;
    }

    static {
        VFT_CONNECTION_PROPERTIES = new TableFormat(1, 1);

        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<stepTime><L><A=500><D=Период опроса устройств>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<giveTime><L><A=10000><D=Интервал обновления информации о переменных>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<liveTime><L><A=10000><D=Интервал обновления отметки жизни>"));
        VFT_CONNECTION_PROPERTIES.addField(FieldFormat.create("<timeOut><L><A=20000><D=Стандартный тайм-аут ввода/вывода>"));

        VFT_REGISTERS = new TableFormat(true);
        FieldFormat ff = FieldFormat.create("<name><S><D=Имя>");
        ff.getValidators().add(ValidatorHelper.NAME_LENGTH_VALIDATOR);
        ff.getValidators().add(ValidatorHelper.NAME_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create("<description><S><D=Описание>");
        ff.getValidators().add(new LimitsValidator(1, 200));
        ff.getValidators().add(ValidatorHelper.DESCRIPTION_SYNTAX_VALIDATOR);
        VFT_REGISTERS.addField(ff);
        VFT_REGISTERS.addField(FieldFormat.create("<canal><I><A=0><D=Номер канала>"));
        ff = FieldFormat.create("<type><I><D=Тип>");
        ff.setSelectionValues(TypeSelectionValues());
        VFT_REGISTERS.addField(ff);
        VFT_REGISTERS.addField(FieldFormat.create("<uId><I><A=0><D=Идентификатор>"));
        ff = FieldFormat.create("<format><I><D=Формат>");
        ff.setSelectionValues(FormatSelectionValues());
        VFT_REGISTERS.addField(ff);
        ff = FieldFormat.create("<lenght><I><A=2><D=Длина в байтах>");
        ff.setSelectionValues(LenghtSelectionValues());
        VFT_REGISTERS.addField(ff);

        VFT_DEVICES = new TableFormat(true);
        VFT_DEVICES.addField(FieldFormat.create("<controller><I><D=Номер контроллера>"));
        VFT_DEVICES.addField(FieldFormat.create("<IPaddr><S><D=IP address контроллера>"));
        VFT_DEVICES.addField(FieldFormat.create("<port><I><A=502><D=Номер порта>"));
        VFT_DEVICES.addField(FieldFormat.create("<idvlr><S><A=1.1.1><D=Имя контроллера>"));
        VFT_DEVICES.addField(FieldFormat.create("<master><B><A=true><D=Признак мастера протокола>"));

        VFT_SQL = new TableFormat(1, 1);
        VFT_SQL.addField(FieldFormat.create("<url><S><A=jdbc:postgresql://192.168.1.30:5432/spaps><D=Url базы данных>"));
        VFT_SQL.addField(FieldFormat.create("<JDBCDriver><S><A=org.postgresql.Driver><D=Драйвер базы данных>"));
        VFT_SQL.addField(FieldFormat.create("<datas><S><A=data><D=Таблица дампа>"));
        VFT_SQL.addField(FieldFormat.create("<setups><S><A=vlr><D=Таблица переменных>"));
        
        VFT_SQL.addField(FieldFormat.create("<user><S><A=postgres><D=Пользователь>"));
        VFT_SQL.addField(FieldFormat.create("<password><S><A=162747><D=Пароль>"));
        VFT_SQL.addField(FieldFormat.create("<stepSQL><L><A=10000><D=Интервал сохранения>"));
        VFT_SQL.addField(FieldFormat.create("<createDB><B><A=true><D=Создавать БД при запуске>"));
        VFT_SQL.addField(FieldFormat.create("<lenghtDB><L><A=5000000><D=Размер циклического буфера>"));

    }

    class MultiReconect extends AggreGateThread {

        private OmronFinsDeviceDriver fd;
        ThreadManager threadManager = null;

        public MultiReconect(OmronFinsDeviceDriver fd, ThreadManager threadManager) {
            super(threadManager);
            this.threadManager = threadManager;
            threadManager.addThread(this);
            this.fd = fd;
            start();
        }

        @Override
        public void run() {
            do {
                try {
                    DataRecord cp = getDeviceContext().getVariable("connectionProperties", getDeviceContext().getCallerController()).rec();
                    DataTable devs = getDeviceContext().getVariable("devices", getDeviceContext().getCallerController());
                    devs.sort("controller", true);
                    for (DataRecord dev : devs) {
                        CanalMaster ccm = null;
                        for (CanalMaster cm : canals) {
                            if (cm.controller == dev.getInt("controller")) {
                                if (!cm.isMaster()) {
                                    break;
                                }
                                ccm = cm;
                                break;
                            }
                        }
                        if (ccm != null) {
                            boolean flag = false;
                            for (FwMasterDevice md : ccm.mdarray) {
                                if (md.myIP().equals(dev.getString("IPaddr")) && md.myPort() == dev.getInt("port")) {
                                    flag = true;
                                    break;
                                }
                            }
                            if (!flag) {
                                try {
                                    Socket socket = new Socket(InetAddress.getByName(dev.getString("IPaddr")), dev.getInt("port"));
                                    FwMasterDevice md = ccm.addDevice(socket);
                                    if (md != null) {
                                        md.setStepTime(cp.getLong("stepTime"));
                                        md.setStepGiveMe(cp.getLong("giveTime"));
                                        md.setStepLive(cp.getLong("liveTime"));
                                        md.gogo();
                                    }
                                } catch (Exception ex) {
                                }
                            }
                        }
                    }
                    for (CanalMaster cm : canals) {
                        if (!cm.isMaster()) {
                            continue;
                        }
                        for (int idx = 0; idx < cm.mdarray.size(); idx++) {
                            FwMasterDevice dev = cm.mdarray.get(idx);
                            if (dev == null) {
                                continue;
                            }
                            if (dev.isConnected()) {
                                continue;
                            }
                            cm.reconnectDevice(idx);
                            dev = cm.mdarray.get(idx);
                            if (dev == null) {
                                continue;
                            }
                            dev.setStepTime(cp.getLong("stepTime"));
                            dev.setStepGiveMe(cp.getLong("giveTime"));
                            dev.setStepLive(cp.getLong("liveTime"));
                            dev.gogo();
                        }
                    }
                } catch (Exception ex) {
                    Log.CORE.error("Reconnect Exception " + ex.getMessage());
                }
                for (CanalMaster cm : canals) {
                    if (cm.isMaster()) {
                        continue;
                    }
                    for (int idx = 0; idx < cm.mdarray.size(); idx++) {
                        FwMasterDevice dev = cm.mdarray.get(idx);
                        if (!dev.isConnected()) {
                            synchronized (cm.mdarray) {
                                cm.mdarray.remove(idx);
                                cm.socketarray.remove(idx);
                            }
                            break;
                        }
                    }
                }
                try {
                    AggreGateThread.sleep(10000L);
                } catch (InterruptedException ex) {
                    return;
                }

            } while (!isInterrupted());
        }
    }

}
