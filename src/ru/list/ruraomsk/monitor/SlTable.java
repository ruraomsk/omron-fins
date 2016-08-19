/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.monitor;

import javax.swing.table.AbstractTableModel;
import ru.list.ruraomsk.fwlib.FwRegister;
import ru.list.ruraomsk.fwlib.FwRegisters;
import ru.list.ruraomsk.fwlib.FwSlaveDevice;
import ru.list.ruraomsk.fwlib.FwUtil;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
    public class SlTable extends AbstractTableModel
    {
        FwRegisters tableDecode;
        FwSlaveDevice sd;
        public SlTable(FwRegisters tableDecode, FwSlaveDevice sd){
            this.sd=sd;
            this.tableDecode=tableDecode;
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex == 3;
        }

        @Override
        public String getColumnName(int column)
        {
            switch (column)
            {
                case 0:
                    return "Имя";
                case 1:
                    return "Тип";
                case 2:
                    return "uId";
                case 3:
                    return "Значение";
            }
            return "Bad Column";
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            if (columnIndex != 3)
            {
                return;
            }
            int idx = 0;
            Object value=null;
            for (FwRegister reg : tableDecode.getCollection())
            {
                if(sd.getController()!=reg.getController()) continue;
                if (rowIndex == idx++)
                {
                    switch(reg.getType()){
                        case FwUtil.FP_TYPE_BOOL:
                            value=aValue.equals("true");
                            break;
                        case FwUtil.FP_TYPE_INTGER:
                            value=Integer.valueOf((String)aValue);
                            break;
                        case FwUtil.FP_TYPE_FLOAT:
                            value=Float.valueOf((String)aValue);
                            break;
                    }
                    if (reg.isInfo())
                    {
                        sd.setValue(reg.getuId(), value);
                    } else
                    {
                        sd.setCode(reg.getuId(), value);
                    }
                    return;
                }

            }
        }

        @Override
        public int getRowCount()
        {
            int count=0;
            for (FwRegister reg : tableDecode.getCollection())
            {
                if(sd.getController()==reg.getController()) count++;
            }

            return count;
        }

        @Override
        public int getColumnCount()
        {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            int idx = 0;
            for (FwRegister reg : tableDecode.getCollection())
            {
                if(sd.getController()!=reg.getController()) continue;
                if (rowIndex == idx++)
                {
                    switch (columnIndex)
                    {
                        case 0:
                            return Monitor.globalName.get(reg.getKey());
                        case 1:
                            return reg.isInfo() ? "Информация" : "Диагностика";
                        case 2:
                            return reg.getuId();
                        case 3:
                            return reg.isInfo() ? sd.getValue(reg.getuId()) : sd.getCode(reg.getuId());
                    }
                    return null;
                }

            }
            return null;
        }
    }
