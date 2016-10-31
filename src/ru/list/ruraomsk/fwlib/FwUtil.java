/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.list.ruraomsk.fwlib;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Русинов Юрий <ruraomsk@list.ru>
 */
public class FwUtil
{

    /**
     * Вычисление контрольной суммы
     *
     * @param buffer
     * @param pos
     * @param len
     * @return контрольную сумму
     */
    public static final int Crc(byte[] buffer, int pos, int len)
    {
        if ((len & 1) > 0) {
            buffer[pos + len] = 0;
            len++;
        }
        int crc = 0;
        for (int i = 0; i < (len >> 1); i++) {

            crc += (ToShort(buffer, pos) & 0xffff);
            crc = crc & 0xffff;
            pos += 2;
        }
        return crc & 0x7fff;
    }

    /**
     * Из буфера в короткое целое
     *
     * @param bytes
     * @param idx
     * @return
     */
    public static final int ToShort(byte bytes[], int idx)
    {
        return (int) ((bytes[idx + 1] << 8) | (bytes[idx] & 0xff));
    }

    /**
     * Целое в иассив байтов
     *
     * @param v
     * @return
     */
    public static final byte[] intToRegisters(int v)
    {
        byte registers[] = new byte[4];
        registers[3] = (byte) (0xff & v >> 24);
        registers[2] = (byte) (0xff & v >> 16);
        registers[1] = (byte) (0xff & v >> 8);
        registers[0] = (byte) (0xff & v);
        return registers;
    }

    /**
     * Целое в буфер записывает два байта
     *
     * @param bytes
     * @param idx
     * @param var
     */
    public static final void IntToBuff(byte bytes[], int idx, int var)
    {
        bytes[idx + 1] = (byte) (0xff & (var >> 8));
        bytes[idx] = (byte) (0xff & var);
    }

    /**
     * Длинное целое в буфкр массива байтов восемь байт
     *
     * @param bytes
     * @param idx
     * @param var
     */
    public static final void LongToBuff(byte bytes[], int idx, long var)
    {
        bytes[idx + 7] = (byte) (0xff & (var >> 56));
        bytes[idx + 6] = (byte) (0xff & (var >> 48));
        bytes[idx + 5] = (byte) (0xff & (var >> 40));
        bytes[idx + 4] = (byte) (0xff & (var >> 32));
        bytes[idx + 3] = (byte) (0xff & (var >> 24));
        bytes[idx + 2] = (byte) (0xff & (var >> 16));
        bytes[idx + 1] = (byte) (0xff & (var >> 8));
        bytes[idx] = (byte) (0xff & var);
    }

    /**
     * Из буфера 4 байта в плавающее
     *
     * @param bytes
     * @param idx
     * @return
     */
    public static final float ToFloat(byte bytes[], int idx)
    {
        return Float.intBitsToFloat((bytes[idx + 3] & 0xff) << 24 | (bytes[idx + 2] & 0xff) << 16 | (bytes[idx + 1] & 0xff) << 8 | bytes[idx] & 0xff);
    }

    /**
     * Плавающее в буфер 4 байта
     *
     * @param bytes
     * @param idx
     * @param f
     */
    public static final void floatToBuff(byte bytes[], int idx, float f)
    {
        byte registers[] = intToRegisters(Float.floatToIntBits(f));
        System.arraycopy(registers, 0, bytes, idx, registers.length);
    }

    /**
     * Из буфера извлекает длинное целое
     *
     * @param bytes
     * @param idx
     * @return
     */
    public static final long ToLong(byte bytes[], int idx)
    {
        return (long) (bytes[idx + 7] & 0xff) << 56 | (long) (bytes[idx + 6] & 0xff) << 48
                | (long) (bytes[idx + 5] & 0xff) << 40 | (long) (bytes[idx + 4] & 0xff) << 32
                | (long) (bytes[idx + 3] & 0xff) << 24 | (long) (bytes[idx + 2] & 0xff) << 16
                | (long) (bytes[idx + 1] & 0xff) << 8 | (long) (bytes[idx] & 0xff);
    }
    public static final long ToTime(byte bytes[], int idx)
    {
        return (long) (bytes[idx + 3] & 0xff) << 56 | (long) (bytes[idx + 2] & 0xff) << 48
                | (long) (bytes[idx + 1] & 0xff) << 40 | (long) (bytes[idx + 0] & 0xff) << 32
                | (long) (bytes[idx + 7] & 0xff) << 24 | (long) (bytes[idx + 6] & 0xff) << 16
                | (long) (bytes[idx + 5] & 0xff) << 8 | (long) (bytes[idx + 4] & 0xff);
    }

    public static byte getVersion(){
        return 0x21;
    }
    public static boolean FP_DEBUG = true;
    public static int MAX_LEN = 64000;
    public static int MIN_LEN = 1;
    public static int VALUE_UIDS = 1000;     // Примерное колличество переменных на устройстве
    public static String textError = "Not Error!";
    public static final byte FP_CODE_INFO = 1;

    public static final byte FP_CODE_DISCR = 3;   // включить имитатор дискрета 
    public static final byte FP_CODE_ANALOG = 4;  // включить имитатор аналоговых сигналов
    
    
    public static final byte FP_CODE_34H = 0x34;    //Передача настроечных данных по инициативе серверов уровня 2 или запрос о настроечных данных в контроллере
    public static final byte FP_CODE_35H = 0x35;     //Квитирование полученных настроечных данных
    public static final byte FP_CODE_36H = 0x36;    //Передача настроечных данных из контроллера на серверы уровня 2 по запросу поступившему от серверов уровня 2
    public static final byte FP_CODE_30H = 0x30;     //Диагностика
    public static final byte FP_CODE_10H = 0x10;     //Управление
    public static final byte FP_CODE_91H = (byte) 0x91;     //Синхронизация времени    
    public static final byte FP_CODE_64H = 0x64;    //Контрольное сообщение от серверов уровня 2    

    public static final int FP_TYPE_BOOL = 0;        //boolean
    public static final int FP_TYPE_INTGER = 1;        //integer
    public static final int FP_TYPE_FLOAT = 2;        //float

    static final int FP_CTRL_ALLINFO = 1;     //получение информации о состоянии всех сигналов контроллера (код 1);
    static final int FP_CTRL_METR = 2;        //переключение контроллера в режим метрологических испытаний каналов измере-ния (код 2);
    static final int FP_CTRL_STANDART = 3;    //переключение контроллера в режим нормального функционирования (код 3);
    static final int FP_CTRL_CHPERIOD = 4;    //изменение периода передачи информационного сообщения на серверы уровня 2 и периода отправки диагностического сообщения (код 4);
    static final int FP_CTRL_RESTART = 5;     //перезагрузка контроллера (код 5);
    static final int FP_CTRL_TESTSYNC = 6;    //проверка синхронизации (код 6);
    static final int FP_CTRL_GOOD = 7;        //проверка целостности программного обеспечения (ПО) (код 7).
    static final int FP_CTRL_ALL = 8;         //установить новый период отправки информации о состоянии всех сигналов контроллера в циклах (примероно по 10мс)
    static final int FP_CYCLE_CONTRL = 10;     //  цикл контроллера в мс
    
    static final long FP_STEP_TIME = 500L;   // стандартная временная задерка для потоков
    static final long FP_STEP_DIAG = 10000L;  // стандартная временная задерка для отправки состояния диагностики
    static final long FP_STEP_GIVEME = 10000L;  // стандартная временная задерка для отправки всех значений инфо
    static final long FP_STEP_LIVE = 10000L;  // стандартная временная задерка для отправки сообщений о живучести

    static final byte FP_DATA_GOOD = 0x0;     // данные достоверные
    static final byte FP_DATA_NOGOOD = 0xf;   // данные совсем плохие
    static final long FP_FLAGINFO = 1048576L;

    static final byte[] buffer = new byte[FwUtil.MAX_LEN];
    static final byte[] crcbuffer = new byte[FwUtil.MAX_LEN];

    static final byte[] outbuf = new byte[FwUtil.MAX_LEN];

    public static int FW_CMD_CANCEL     = 0;
    public static int FW_CMD_NEXT       = 1;
    public static int FW_CMD_LAST       = 3;
    public static int FW_CMD_RECIVE     = 5;

    public static int FW_CMD_SIZE     = 200;

    /**
     * таблица всех слэйв устройств в системе ключ номер порта
     */
    static ConcurrentHashMap<Integer, FwSlaveDevice> S_DEV = new ConcurrentHashMap<Integer, FwSlaveDevice>();

}
