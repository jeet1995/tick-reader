package com.tickreader.entity;

import java.time.LocalDateTime;

public class Tick {

    private String id;
    private String pk;

    private String ricName;

    private long messageTimestamp;
    private int msgSequence;
    private long recordKey;

    private LocalDateTime collectDateTime;
    private int rtlWrap;
    private int rtl;

    private LocalDateTime sourceDateTime;
    private int seqNum;

    private int bid;
    private int bidSize;

    private String noBidMkr;

    private int ask;
    private int askSize;

    private String noAskMkr;

    private double midPrice;

    private double impVoltA;
    private double impVoltB;

    private double delta;
    private double vega;
    private double theta;
    private double rho;
    private double gamma;

    private double currAsk;
    private DocType docType;

    public int getRtlWrap() {
        return rtlWrap;
    }

    public void setRtlWrap(int rtlWrap) {
        this.rtlWrap = rtlWrap;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public String getRicName() {
        return ricName;
    }

    public void setRicName(String ricName) {
        this.ricName = ricName;
    }

    public long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(long messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    public int getMsgSequence() {
        return msgSequence;
    }

    public void setMsgSequence(int msgSequence) {
        this.msgSequence = msgSequence;
    }

    public long getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(long recordKey) {
        this.recordKey = recordKey;
    }

    public LocalDateTime getCollectDateTime() {
        return collectDateTime;
    }

    public void setCollectDateTime(LocalDateTime collectDateTime) {
        this.collectDateTime = collectDateTime;
    }

    public int getRtl() {
        return rtl;
    }

    public void setRtl(int rtl) {
        this.rtl = rtl;
    }

    public LocalDateTime getSourceDateTime() {
        return sourceDateTime;
    }

    public void setSourceDateTime(LocalDateTime sourceDateTime) {
        this.sourceDateTime = sourceDateTime;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public int getBidSize() {
        return bidSize;
    }

    public void setBidSize(int bidSize) {
        this.bidSize = bidSize;
    }

    public String getNoBidMkr() {
        return noBidMkr;
    }

    public void setNoBidMkr(String noBidMkr) {
        this.noBidMkr = noBidMkr;
    }

    public int getAsk() {
        return ask;
    }

    public void setAsk(int ask) {
        this.ask = ask;
    }

    public int getAskSize() {
        return askSize;
    }

    public void setAskSize(int askSize) {
        this.askSize = askSize;
    }

    public String getNoAskMkr() {
        return noAskMkr;
    }

    public void setNoAskMkr(String noAskMkr) {
        this.noAskMkr = noAskMkr;
    }

    public double getMidPrice() {
        return midPrice;
    }

    public void setMidPrice(double midPrice) {
        this.midPrice = midPrice;
    }

    public double getImpVoltA() {
        return impVoltA;
    }

    public void setImpVoltA(double impVoltA) {
        this.impVoltA = impVoltA;
    }

    public double getImpVoltB() {
        return impVoltB;
    }

    public void setImpVoltB(double impVoltB) {
        this.impVoltB = impVoltB;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta(double delta) {
        this.delta = delta;
    }

    public double getVega() {
        return vega;
    }

    public void setVega(double vega) {
        this.vega = vega;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getCurrAsk() {
        return currAsk;
    }

    public void setCurrAsk(double currAsk) {
        this.currAsk = currAsk;
    }

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    @Override
    public String toString() {
        return "Tick{" +
                "id='" + id + '\'' +
                ", pk='" + pk + '\'' +
                ", ricName='" + ricName + '\'' +
                ", messageTimestamp=" + messageTimestamp +
                ", msgSequence=" + msgSequence +
                ", recordKey=" + recordKey +
                ", collectDateTime=" + collectDateTime +
                ", rtlWrap=" + rtlWrap +
                ", rtl=" + rtl +
                ", sourceDateTime=" + sourceDateTime +
                ", seqNum=" + seqNum +
                ", bid=" + bid +
                ", bidSize=" + bidSize +
                ", noBidMkr='" + noBidMkr + '\'' +
                ", ask=" + ask +
                ", askSize=" + askSize +
                ", noAskMkr='" + noAskMkr + '\'' +
                ", midPrice=" + midPrice +
                ", impVoltA=" + impVoltA +
                ", impVoltB=" + impVoltB +
                ", delta=" + delta +
                ", vega=" + vega +
                ", theta=" + theta +
                ", rho=" + rho +
                ", gamma=" + gamma +
                ", currAsk=" + currAsk +
                ", docType=" + docType +
                '}';
    }
}
