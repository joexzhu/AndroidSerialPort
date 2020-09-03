package com.deemons.androidserialport.activity;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.Utils;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.deemons.androidserialport.R;
import com.deemons.androidserialport.bean.LeftDetailBean;
import com.deemons.androidserialport.bean.LeftHeadBean;
import com.deemons.androidserialport.bean.MessageBean;
import com.deemons.serialportlib.ByteUtils;
import com.deemons.serialportlib.SerialPort;
import com.deemons.serialportlib.SerialPortFinder;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;

/**
 * author： deemons
 * date:    2018/5/26
 * desc:
 */
public class MainPresenter implements MainContract.IPresenter {

    private final String mDateFormat = "HH:mm:ss:SSS";
    MainContract.IView mView;

    private LinkedHashSet<String> mSendHistory;

    private String mPath;
    private int    mBaudRate;
    private int    mCheckDigit;
    private int    mDataBits;
    private int    mStopBit;

    private boolean isHexReceive;
    private boolean isHexSend;
    private boolean isShowSend;
    private boolean isShowTime;
    private int     mRepeatDuring;

    private SerialPort                mSerialPort;
    private boolean                   isInterrupted;
    private Disposable                mReceiveDisposable;
    private ObservableEmitter<String> mEmitter;
    private Disposable                mSendDisposable;
    private Disposable                mSendRepeatDisposable;

    public MainPresenter(MainContract.IView view) {
        mView = view;

        isHexReceive = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_TYPE, true);
        isHexSend = SPUtils.getInstance().getBoolean(SPKey.SETTING_SEND_TYPE, true);
        isShowSend = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_SHOW_SEND, true);
        isShowTime = SPUtils.getInstance().getBoolean(SPKey.SETTING_RECEIVE_SHOW_TIME, true);
        mRepeatDuring = SPUtils.getInstance().getInt(SPKey.SETTING_SEND_DURING, 1000);

        try {
            mSendHistory = (LinkedHashSet<String>) SPUtils.getInstance()
                .getStringSet(SPKey.SEND_HISTORY, new LinkedHashSet<String>(30));
        } catch (ClassCastException e) {
            e.printStackTrace();
            mSendHistory = new LinkedHashSet<String>(30);
        }
    }

    private void refreshValueFormSp() {
        mPath = SPUtils.getInstance().getString(SPKey.SERIAL_PORT, "");
        mBaudRate = Integer.parseInt(SPUtils.getInstance().getString(SPKey.BAUD_RATE, "9600"));
        mCheckDigit = Integer.parseInt(SPUtils.getInstance().getString(SPKey.CHECK_DIGIT, "0"));
        mDataBits = Integer.parseInt(SPUtils.getInstance().getString(SPKey.DATA_BITS, "8"));
        mStopBit = Integer.parseInt(SPUtils.getInstance().getString(SPKey.STOP_BIT, "1"));
    }

    public void getLeftData() {
        refreshValueFormSp();

        ArrayList<MultiItemEntity> list = new ArrayList<>();

        list.add(getLeftSerialPortBean());
        list.add(getLeftBaudRateBean());
        list.add(getLeftCheckDigitBean());
        list.add(getLeftDataBitsBean());
        list.add(getLeftStopBitsBean());

        mView.setLeftData(list);
    }

    @NonNull
    private LeftHeadBean getLeftSerialPortBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        SerialPortFinder finder = new SerialPortFinder();
        String[] path = finder.getAllDevicesPath();
        for (String s : path) {
            if (TextUtils.isEmpty(mPath)) {
                mPath = s;
                SPUtils.getInstance().put(SPKey.SERIAL_PORT, mPath);
            }

            if (s.equals(mPath)) {
                list.add(new LeftDetailBean(s, true));
            } else {
                list.add(new LeftDetailBean(s));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_serial_port;
        bean.title = "串口";
        bean.spKey = SPKey.SERIAL_PORT;
        bean.value = mPath;

        for (LeftDetailBean detailBean : list) {
            bean.addSubItem(detailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftBaudRateBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.baud_rate);
        for (int i : array) {
            if (i == mBaudRate) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_baud;
        bean.title = "波特率";
        bean.spKey = SPKey.BAUD_RATE;
        bean.value = String.valueOf(mBaudRate);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftCheckDigitBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.check_digit);
        for (int i : array) {
            if (i == mCheckDigit) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_check;
        bean.title = "校验位";
        bean.spKey = SPKey.CHECK_DIGIT;
        bean.value = String.valueOf(mCheckDigit);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftDataBitsBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.data_bits);
        for (int i : array) {
            if (i == mStopBit) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_data;
        bean.title = "数据位";
        bean.spKey = SPKey.DATA_BITS;
        bean.value = String.valueOf(mDataBits);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    private LeftHeadBean getLeftStopBitsBean() {
        ArrayList<LeftDetailBean> list = new ArrayList<>();

        int[] array = Utils.getApp().getResources().getIntArray(R.array.stop_bits);
        for (int i : array) {
            if (i == mStopBit) {
                list.add(new LeftDetailBean(String.valueOf(i), true));
            } else {
                list.add(new LeftDetailBean(String.valueOf(i)));
            }
        }

        LeftHeadBean bean = new LeftHeadBean();
        bean.imageRes = R.mipmap.ic_stop;
        bean.title = "停止位";
        bean.spKey = SPKey.STOP_BIT;
        bean.value = String.valueOf(mStopBit);

        for (LeftDetailBean leftDetailBean : list) {
            bean.addSubItem(leftDetailBean);
        }

        return bean;
    }

    public void open() {
        refreshValueFormSp();
        try {
            mSerialPort =
                new SerialPort(new File(mPath), mBaudRate, mCheckDigit, mDataBits, mStopBit, 0);
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.showLong("打开失败！请尝试其它串口！");
        } catch (SecurityException e) {
            e.printStackTrace();
            mView.showPermissionDialog();
        }

        if (mSerialPort != null) {
            isInterrupted = false;
            onReceiveSubscribe();
            onSendSubscribe();
            ToastUtils.showLong("打开串口成功！");
        }

        mView.setOpen(mSerialPort != null);
    }

    private void onSendSubscribe() {
        mSendDisposable =
            Observable.create((ObservableOnSubscribe<String>) emitter -> mEmitter = emitter)
                .filter(s -> !TextUtils.isEmpty(s))
                .doOnNext(s -> mSendHistory.add(s))
                .doOnNext(s -> mSerialPort.getOutputStream()
                    .write(isHexSend ? ByteUtils.hexStringToBytes(s)
                        : ByteUtils.stringToAsciiBytes(s)))
                .observeOn(AndroidSchedulers.mainThread())
                .filter(s -> isShowSend)
                .subscribe(s -> {
                    mView.addData(new MessageBean(MessageBean.TYPE_SEND,
                        isShowTime ? DateTime.now().toString(mDateFormat) : "", s));
                }, Throwable::printStackTrace);
    }

    public void close() {
        isInterrupted = true;
        disposable(mReceiveDisposable);

        disposable(mSendDisposable);
        mEmitter = null;

        disposable(mSendRepeatDisposable);

        mSerialPort = null;

        mView.setOpen(false);
    }

    private void onReceiveSubscribe() {
        mReceiveDisposable = Flowable.create((FlowableOnSubscribe<byte[]>) emitter -> {
            InputStream is = mSerialPort.getInputStream();
            int available;
            int first;
            while (!isInterrupted
                && mSerialPort != null
                && is != null
                && (first = is.read()) != -1) {
                do {
                    available = is.available();
                    SystemClock.sleep(1);
                } while (available != is.available());

                available = is.available();
                byte[] bytes = new byte[available + 1];
                is.read(bytes, 1, available);
                bytes[0] = (byte) (first & 0xFF);
                emitter.onNext(bytes);
            }
            close();
        }, BackpressureStrategy.MISSING)
            .retry()
            .map(bytes -> isHexReceive ? addSpace(ByteUtils.bytesToHexString(bytes))
                : ByteUtils.bytesToAscii(bytes))
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(it -> mView.addData(new MessageBean(MessageBean.TYPE_RECEIVE,
                    isShowTime ? DateTime.now().toString(mDateFormat) : "", it)),
                Throwable::printStackTrace);
    }

    private String addSpace(String s) {
        if (s.length() % 2 == 0) {
            StringBuilder builder = new StringBuilder();
            char[] array = s.toCharArray();
            int length = array.length;
            for (int i = 0; i < length; i += 2) {
                if (i != 0 && i <= length - 2) {
                    builder.append(" ");
                }

                builder.append(array[i]);
                builder.append(array[i + 1]);
            }

            return builder.toString();
        }
        return s;
    }

    public void sendMsg(String contain) {
        if (mEmitter != null) {
            mEmitter.onNext(contain.replace(" ", ""));
        } else {
            ToastUtils.showLong("请先打开串口！");
        }
    }

    public void refreshSendDuring(int result) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_DURING, result);
        mRepeatDuring = result;

        //正在运行
        if (mSendRepeatDisposable != null && !mSendRepeatDisposable.isDisposed()) {
            registerSendRepeat(true);
        }
    }

    public void refreshSendRepeat(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_REPEAT, isChecked);

        registerSendRepeat(isChecked);
    }

    public void refreshShowTime(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_SHOW_TIME, isChecked);
        isShowTime = isChecked;
    }

    public void refreshShowSend(boolean isChecked) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_SHOW_SEND, isChecked);
        isShowSend = isChecked;
    }

    public void refreshSendType(boolean isHex) {
        SPUtils.getInstance().put(SPKey.SETTING_SEND_TYPE, isHex);
        isHexSend = isHex;
    }

    public void refreshReceiveType(boolean isHex) {
        SPUtils.getInstance().put(SPKey.SETTING_RECEIVE_TYPE, isHex);
        isHexReceive = isHex;
    }

    private void registerSendRepeat(boolean checked) {
        if (mSerialPort == null) {
            ToastUtils.showLong("请先打开串口！");
            return;
        }

        disposable(mSendRepeatDisposable);

        if (checked) {
            mSendRepeatDisposable = Observable.interval(mRepeatDuring, TimeUnit.MILLISECONDS)
                .subscribe(aLong -> sendMsg(mView.getEditText()), Throwable::printStackTrace);
        }
    }

    public void onDestroy() {
        close();
        SPUtils.getInstance().put(SPKey.SEND_HISTORY, mSendHistory);
    }

    private void disposable(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public ArrayList<String> getHistory() {
        return new ArrayList<>(mSendHistory);
    }
}
