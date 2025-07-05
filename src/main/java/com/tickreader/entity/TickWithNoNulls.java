package com.tickreader.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TickWithNoNulls extends BaseTick {

    private String id;
    private String pk;
    private String ricName;
    private Long messageTimestamp;
    private Long executionTime;
    private Integer msgSequence;
    private Long RecordKey;
    private Long COLLECT_DATETIME;
    private Integer RTL_Wrap;
    private Long RTL;
    private String Sub_RTL;
    private String RuleSetVersion;
    private String RuleID;
    private String RuleVersionID;
    private String RuleClauseNo;
    private String RecordType;
    private String RecordStatus;
    private String EditType;
    private Long SOURCE_DATETIME;
    private String SEQNUM;
    private String TRDXID_1;
    private Double BID;
    private Double BIDSIZE;
    private String BID_MMID1;
    private Long NO_BIDMMKR;
    private Double ASK;
    private Double ASKSIZE;
    private String ASK_MMID1;
    private Long NO_ASKMMKR;
    private Double MID_PRICE;
    private String DSPLY_NAME;
    private Double STRIKE_PRC;
    private Double YLDTOMAT;
    private String BUYER_ID;
    private String SELLER_ID;
    private Double BID_YIELD;
    private Double ASK_YIELD;
    private String SRC_REF1;
    private Double SWAP_RATE;
    private String DLG_CODE1;
    private String CTBTR_1;
    private String CTB_LOC1;
    private Double FIXING_1;
    private Double CNV_PARITY;
    private Double PREMIUM;
    private Double CONV_FAC;
    private Double YLD_TO_CLL;
    private Double YLD_TO_PUT;
    private Double SMP_MARGIN;
    private Double DSC_MARGIN;
    private Double IMP_VOLT;
    private Double IMP_VOLTA;
    private Double IMP_VOLTB;
    private Double OAS;
    private Double REF_YIELD;
    private Double DELTA;
    private Double IMP_REPO;
    private Double SWAP_SPRD;
    private Double SWAP_YLD;
    private Double ASK_SPREAD;
    private Double AST_SWPSPD;
    private Double BID_SPREAD;
    private Double BMK_SPD;
    private Double BPV;
    private Double DISC_RATE;
    private Double DISC_MRGA;
    private Double DISC_MRGB;
    private Double ISMA_B_YLD;
    private Double ISMA_A_YLD;
    private Double MID_SPREAD;
    private Double MID_YLD_1;
    private Double ASTSWPSD_A;
    private Double ASTSWPSD_B;
    private Long FIX_DATE;

    @JsonProperty("30D_A_IM_C")
    private Double ThirtyD_A_IM_C;

    @JsonProperty("30D_A_IM_P")
    private Double ThirtyD_A_IM_P;

    @JsonProperty("60D_A_IM_C")
    private Double SixtyD_A_IM_C;

    @JsonProperty("60D_A_IM_P")
    private Double SixtyD_A_IM_P;

    @JsonProperty("90D_A_IM_C")
    private Double NinetyD_A_IM_C;

    @JsonProperty("90D_A_IM_P")
    private Double NinetyD_A_IM_P;
    private Double CDS_BASIS;
    private Double CDS_DV01;
    private Double BEVEN_INF;
    private Double REAL_YLDA;
    private Double REAL_YLDB;
    private Double ZSPREAD;
    private Double BMK_SPDA;
    private Double BMK_SPDB;
    private Double BMK_YIELD;
    private Double BP_VOLT;
    private Double CAP_PREM;
    private Double CNV_EDGE;
    private Double CONVX_BIAS;
    private Double DISC_FACTR;
    private Double FAIR_PRICE;
    private Double FC_AVSWPTS;
    private Double FCAST_MAX;
    private Double FCAST_MEAN;
    private Double FCAST_MED;
    private Double FCAST_MIN;
    private Double FCAST_STDV;
    private Double FLOOR_PREM;
    private Double FUT_BASIS;
    private Double FUT_RISK;
    private Double FWD_OR_ASK;
    private Double FWD_OR_BID;
    private Double IMP_YIELD;
    private Double INDEX_SKEW;
    private Double MEAN_REV;
    private Long RESR_VOL;
    private Double SH_RT_VOLT;
    private Double SWAP_PREM;
    private Double SWAP_SPRDA;
    private Double SWAP_SPRDB;
    private Double YLDTOBEST;
    private Double YLDTOWORST;
    private Double LAST_QUOTE;
    private Double DISC_BID1;
    private Double DISC_ASK1;
    private Double BOND_FLOOR;
    private Double DURATION;
    private Double THETA;
    private Double GAMMA;
    private Double CONVEXITY;
    private Double IMP_CORR;
    private Double RUN_SPRD;
    private Double BASE_CORR;
    private Double PAR_YLD;
    private Double ZERO_YLD;
    private Double PRC_VOLTY;
    private Long ChangeTimeStamp;
    private Long ChangeSequence;
    private Integer TAQFILTER;
    private Double RECOV_RATE;
    private Double DEFLT_PROB;
    private Double OAS_BID;
    private Double OAS_ASK;
    private Double YTM_BID;
    private Double YTM_ASK;
    private Double PV01;
    private Double UPLIMIT;
    private Double LOLIMIT;
    private Double CMP_YLD_B;
    private Double CMP_YLD_A;
    private Double ASP1M;
    private Double ASP3M;
    private Double ASP6M;
    private Double CPN_FIXED1;
    private Double THEO_SPD_B;
    private Double THEO_SPD_M;
    private Double THEO_SPD_A;
    private Double THEO_PRC_B;
    private Double THEO_PRC_M;
    private Double THEO_PRC_A;
    private Double CARRY_COST;
    private Double ROLL_DOWN;
    private Double CRD_TOTAL;
    private Double FWD_RATE;
    private Double CM_YLD;
    private Long B_DLR_CNT;
    private Long A_DLR_CNT;
    private Double SOV_SPREAD;
    private Long AC_BID_ORD;
    private Long AC_ASK_ORD;
    private Long AC_BORD_SZ;
    private Long AC_AORD_SZ;
    private String LIMIT_INDQ;
    private String SH_SAL_RES;
    private String TRD_STATUS;
    private String HALT_RSN;
    private Double NETCHNG_1;
    private Double EFF_DURTN;
    private Double EFF_CONVX;
    private Double ESPRD_TSRY;
    private Double FV_SPREAD;
    private Double FV_YIELD;
    private Double FV_SCORE;
    private Double FV_SCR_DEV;
    private Double FV_DV01;
    private Double THEO_PRC;
    private Double GRS_BASIS;
    private Double FWD_RISK;
    private Double HEDGERATIO;
    private Double NET_BASIS;
    private Double INV_PRC;
    private Double INV_SPD;
    private Double IMB_PR_FR;
    private Double IMB_PR_NR;
    private Double IMB_PR_REF;
    private Long IMB_PR_SH;
    private Long IMB_SH;
    private String IMB_SIDE;
    private String IMB_VA_IND;
    private String IMB_ACT_TP;
    private Double FCAST_HIGH;
    private Double FCAST_LOW;
    private Double FCAST_MEDN;
    private Double ASK_FWDORT;
    private Double BID_FWDORT;
    private Double PAR_YLD1;
    private Double RUN_SPREAD;
    private Double ZERO_YLD1;
    private Double BMK_SPDASK;
    private Double BMK_SPDBID;
    private Double BOND_FLR;
    private Double CNV_EDGE1;
    private Double YLDBST;
    private Double YLDWST;
    private Double OPEN_PRC;
    private Double HIGH_1;
    private Double LOW_1;
    private Double OPEN_YLD;
    private Double HIGH_YLD;
    private Double LOW_YLD;
    private Double BENCH_PRC;
    private String BKGD_REF;
    private Double NRG_CRACK;
    private Double NRG_FRGHT;
    private Double NRG_TOP;
    private Double TRDVOL_1;
    private Double YIELD;
    private String BID_TICK_1;
    private Double INT_BASIS;
    private Double INT_CDS;
    private Double MOD_DURTN;
    private Double SWP_POINT;
    private Double CLEAN_PRC;
    private String ISIN_CODE;
    private String MIC_CODE;
    private String MIFIR_ID;
    private String MIFIR_U_AS;
    private String MIFIR_C_TP;
    private Long SOURCE_DATETIME_EXT;
    private Double QUOTE_VAL;
    private String QTE_ID;
    private Double QUOTE_SIZE;
    private String ISIN_CD_D;
    private Double TRTN_PRICE;
    private Double CNV_PREM;
    private Double CNV_RATIO;
    private Double CURR_BID;
    private Double CURR_ASK;
    private String EXCH_DATE;
    private String EXCH_TIME;
    private String SUM_ACTION_1;
    private String SUM_ACTION_2;
    private String SUM_ACTION_3;
    private String SUM_ACTION_4;
    private String SUM_ACTION_5;
    private Double RHO;
    private Double VEGA;
    private String qualifiers;
    private String user_qualifiers;
    private String messageTimestamp_st;
    private String executionTime_st;
    private String COLLECT_DATETIME_st;
    private String SOURCE_DATETIME_st;
    private String ChangeTimeStamp_st;
    private java.time.LocalDateTime messageTimestamp_dt;
    private java.time.LocalDateTime executionTime_dt;
    private java.time.LocalDateTime COLLECT_DATETIME_dt;
    private java.time.LocalDateTime SOURCE_DATETIME_dt;
    private java.time.LocalDateTime ChangeTimeStamp_dt;
    private Integer messageTimestamp_ns;
    private Integer executionTime_ns;
    private Integer COLLECT_DATETIME_ns;
    private Integer SOURCE_DATETIME_ns;
    private Integer ChangeTimeStamp_ns;
    private String FIX_DATE_st;
    private java.time.LocalDateTime FIX_DATE_dt;
    private String FIX_DATE_ns;
    private Double TRDPRC_1;
    private Double ACVOL_1;
    private Double VWAP;
    private String PRCTCK_1;
    private Long BLKVOLUM;
    private Long TOT_VOLUME;
    private Long VOLUME_ADV;
    private Long VOLUME_DEC;
    private Long VOLUME_UNC;
    private Long ISSUES_ADV;
    private Long ISSUES_DEC;
    private Long ISSUES_UNC;
    private Long NEW_LOWS;
    private Long TOT_ISSUES;
    private Long NEW_HIGHS;
    private Double NRG_NTBACK;
    private Double NRG_SWING;
    private Double CMP_YIELD;
    private Double ORDBK_VWAP;
    private Double NAV;
    private Long EFS_VOL;
    private Long EFP_VOL;
    private Double COM_BASIS;
    private Double FPN;
    private Long MAX_EX_LMT;
    private Long MAX_IM_LMT;
    private Long NRG_VAR;
    private Double QPN;
    private Double REFM_CRACK;
    private Double REFM_TOP;
    private Double SSN_DEMAND;
    private Double OFFER;
    private String ECON_ACT;
    private Double AVERG_PRC;
    private Long OPINT_1;
    private Long PRD_NUMMOV;
    private Long TOT_DEMAND;
    private Double PCTCHNG;
    private String TRADE_ID;
    private Double QUTA_REM;
    private Double ODD_PRC;
    private Long ODD_TRDVOL;
    private Double ODD_TURN;
    private Double TOT_BUYVAL;
    private Long TOT_BUYVOL;
    private Double TOT_SELVAL;
    private Long TOT_SELVOL;
    private String TRD_P_CCY;
    private Double FLOWS;
    private Double TRNOVR_UNS;
    private Double PRIMACT_1;
    private Double TRD_YLD1;
    private Double FAIR_VALUE;
    private Double FV_CAL_VOL;
    private Double IND_AUC;
    private Double IND_AUCVOL;
    private Long SEQ_NO;
    private String TRD_RIC;
    private String MMT_CLASS;
    private String INST_DESC;
    private String TR_TRD_FLG;
    private String AGGRS_SID1;
    private Double TRTN_1W;
    private Double TRTN_1M;
    private Double TRTN_2Y;
    private Double TRTN_3Y;
    private Double TRTN_4Y;
    private Double TRTN_5Y;
    private Double MTD_TRTN;
    private Double QTD_TRTN;
    private Double TRTN;
    private Double YR_TRTN;
    private Double YTD_TRTN;
    private Double TRTN_3MT;
    private Double O_TRDPRC;
    private Double O_TRDVOL;
    private Double ACVOL_UNS;
    private Double ECON_FCAST;
    private Double ECON_PRIOR;
    private Double ECON_REV;
    private Double FCAST_NUM;
    private Double MKT_CHNG;
    private Double MKT_STRN;
    private Double MKT_VOLT;
    private Double MKT_WEAK;
    private Long MOVES_ADV;
    private Long MOVES_DEC;
    private Long MOVES_UNC;
    private Double PERATIO;
    private Long TOT_MOVES;
    private Double FVMA_1MM;
    private Double FVMA_3MM;
    private Double FVMA_5MM;
    private Double FVMA_10MM;
    private Double FVMA_20MM;
    private Double FVMA_30MM;
    private Double FVMA_40MM;
    private Double FVMA_50MM;
    private Double FVMA_60MM;
    private Double FVMA_70MM;
    private Double FVMA_80MM;
    private Double FVMA_90MM;
    private Double FVMA_100M;
    private Double FVAC_1MM;
    private Double FVAC_3MM;
    private Double FVAC_5MM;
    private Double FVAC_10MM;
    private Double FVAC_20MM;
    private Double FVAC_30MM;
    private Double FVAC_40MM;
    private Double FVAC_50MM;
    private Double FVAC_60MM;
    private Double FVAC_70MM;
    private Double FVAC_80MM;
    private Double FVAC_90MM;
    private Double FVAC_100M;
    private String RPT_BS_CCY;
    private String RPT_P_METH;
    private Double LTNOV_UNS;
    private Double TRD_ASP1M;
    private Double TRD_ASP3M;
    private Double TRD_ASP6M;
    private Double TRD_ASP;
    private Double TRD_BPV;
    private Double TRD_BVNINF;
    private Double TRD_CLN_PR;
    private Double TRD_CMPYLD;
    private Double TRD_CNVXTY;
    private Double TRD_ISMAYL;
    private Double TRD_OAS;
    private Double TRD_SWP_SP;
    private Double TRD_DSCMRG;
    private String MFD_TRANTP;
    private String MFD_NGOTRD;
    private String MFD_AGENCY;
    private String MFD_MODTRD;
    private String MFD_REFTRD;
    private String MFD_SP_DIV;
    private String MFD_FRMTRD;
    private String MFD_ALGTRD;
    private String MFD_DEFRSN;
    private String MFD_DEFTYP;
    private String MFD_DUPTRD;
    private Double TRDVOL_ALT;
    private Double TRD_BMKSPD;
    private Double TRD_CDS_BS;
    private Double TRD_SIMMGN;
    private Double TRD_ZSPRD;
    private Double TRD_YTB;
    private Double TRD_YTM;
    private Double TRD_BNDFLR;
    private Double TRD_YTC;
    private Double TRD_YTP;
    private Double TRD_YTW;
    private Double TRD_MODDUR;
    private Double TRD_PREM;
    private Double AGGR_PRC;
    private Double AGGR_VOL;
    private Integer AGGR_CNT;
    private String SUM_ACTION_6;
    private String SUM_ACTION_7;
    private String SUM_ACTION_8;

    public String getRicName() {
        return ricName;
    }

    public void setRicName(String ricName) {
        this.ricName = ricName;
    }

    public Long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(Long messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    public Integer getMsgSequence() {
        return msgSequence;
    }

    public void setMsgSequence(Integer msgSequence) {
        this.msgSequence = msgSequence;
    }

    public Long getRecordKey() {
        return RecordKey;
    }

    public void setRecordKey(Long RecordKey) {
        this.RecordKey = RecordKey;
    }

    public Long getCOLLECT_DATETIME() {
        return COLLECT_DATETIME;
    }

    public void setCOLLECT_DATETIME(Long COLLECT_DATETIME) {
        this.COLLECT_DATETIME = COLLECT_DATETIME;
    }

    public Integer getRTL_Wrap() {
        return RTL_Wrap;
    }

    public void setRTL_Wrap(Integer RTL_Wrap) {
        this.RTL_Wrap = RTL_Wrap;
    }

    public Long getRTL() {
        return RTL;
    }

    public void setRTL(Long RTL) {
        this.RTL = RTL;
    }

    public String getSub_RTL() {
        return Sub_RTL;
    }

    public void setSub_RTL(String Sub_RTL) {
        this.Sub_RTL = Sub_RTL;
    }

    public String getRuleSetVersion() {
        return RuleSetVersion;
    }

    public void setRuleSetVersion(String RuleSetVersion) {
        this.RuleSetVersion = RuleSetVersion;
    }

    public String getRuleID() {
        return RuleID;
    }

    public void setRuleID(String RuleID) {
        this.RuleID = RuleID;
    }

    public String getRuleVersionID() {
        return RuleVersionID;
    }

    public void setRuleVersionID(String RuleVersionID) {
        this.RuleVersionID = RuleVersionID;
    }

    public String getRuleClauseNo() {
        return RuleClauseNo;
    }

    public void setRuleClauseNo(String RuleClauseNo) {
        this.RuleClauseNo = RuleClauseNo;
    }

    public String getRecordType() {
        return RecordType;
    }

    public void setRecordType(String RecordType) {
        this.RecordType = RecordType;
    }

    public String getRecordStatus() {
        return RecordStatus;
    }

    public void setRecordStatus(String RecordStatus) {
        this.RecordStatus = RecordStatus;
    }

    public String getEditType() {
        return EditType;
    }

    public void setEditType(String EditType) {
        this.EditType = EditType;
    }

    public Long getSOURCE_DATETIME() {
        return SOURCE_DATETIME;
    }

    public void setSOURCE_DATETIME(Long SOURCE_DATETIME) {
        this.SOURCE_DATETIME = SOURCE_DATETIME;
    }

    public String getSEQNUM() {
        return SEQNUM;
    }

    public void setSEQNUM(String SEQNUM) {
        this.SEQNUM = SEQNUM;
    }

    public String getTRDXID_1() {
        return TRDXID_1;
    }

    public void setTRDXID_1(String TRDXID_1) {
        this.TRDXID_1 = TRDXID_1;
    }

    public Double getBID() {
        return BID;
    }

    public void setBID(Double BID) {
        this.BID = BID;
    }

    public Double getBIDSIZE() {
        return BIDSIZE;
    }

    public void setBIDSIZE(Double BIDSIZE) {
        this.BIDSIZE = BIDSIZE;
    }

    public String getBID_MMID1() {
        return BID_MMID1;
    }

    public void setBID_MMID1(String BID_MMID1) {
        this.BID_MMID1 = BID_MMID1;
    }

    public Long getNO_BIDMMKR() {
        return NO_BIDMMKR;
    }

    public void setNO_BIDMMKR(Long NO_BIDMMKR) {
        this.NO_BIDMMKR = NO_BIDMMKR;
    }

    public Double getASK() {
        return ASK;
    }

    public void setASK(Double ASK) {
        this.ASK = ASK;
    }

    public Double getASKSIZE() {
        return ASKSIZE;
    }

    public void setASKSIZE(Double ASKSIZE) {
        this.ASKSIZE = ASKSIZE;
    }

    public String getASK_MMID1() {
        return ASK_MMID1;
    }

    public void setASK_MMID1(String ASK_MMID1) {
        this.ASK_MMID1 = ASK_MMID1;
    }

    public Long getNO_ASKMMKR() {
        return NO_ASKMMKR;
    }

    public void setNO_ASKMMKR(Long NO_ASKMMKR) {
        this.NO_ASKMMKR = NO_ASKMMKR;
    }

    public Double getMID_PRICE() {
        return MID_PRICE;
    }

    public void setMID_PRICE(Double MID_PRICE) {
        this.MID_PRICE = MID_PRICE;
    }

    public String getDSPLY_NAME() {
        return DSPLY_NAME;
    }

    public void setDSPLY_NAME(String DSPLY_NAME) {
        this.DSPLY_NAME = DSPLY_NAME;
    }

    public Double getSTRIKE_PRC() {
        return STRIKE_PRC;
    }

    public void setSTRIKE_PRC(Double STRIKE_PRC) {
        this.STRIKE_PRC = STRIKE_PRC;
    }

    public Double getYLDTOMAT() {
        return YLDTOMAT;
    }

    public void setYLDTOMAT(Double YLDTOMAT) {
        this.YLDTOMAT = YLDTOMAT;
    }

    public String getBUYER_ID() {
        return BUYER_ID;
    }

    public void setBUYER_ID(String BUYER_ID) {
        this.BUYER_ID = BUYER_ID;
    }

    public String getSELLER_ID() {
        return SELLER_ID;
    }

    public void setSELLER_ID(String SELLER_ID) {
        this.SELLER_ID = SELLER_ID;
    }

    public Double getBID_YIELD() {
        return BID_YIELD;
    }

    public void setBID_YIELD(Double BID_YIELD) {
        this.BID_YIELD = BID_YIELD;
    }

    public Double getASK_YIELD() {
        return ASK_YIELD;
    }

    public void setASK_YIELD(Double ASK_YIELD) {
        this.ASK_YIELD = ASK_YIELD;
    }

    public String getSRC_REF1() {
        return SRC_REF1;
    }

    public void setSRC_REF1(String SRC_REF1) {
        this.SRC_REF1 = SRC_REF1;
    }

    public Double getSWAP_RATE() {
        return SWAP_RATE;
    }

    public void setSWAP_RATE(Double SWAP_RATE) {
        this.SWAP_RATE = SWAP_RATE;
    }

    public String getDLG_CODE1() {
        return DLG_CODE1;
    }

    public void setDLG_CODE1(String DLG_CODE1) {
        this.DLG_CODE1 = DLG_CODE1;
    }

    public String getCTBTR_1() {
        return CTBTR_1;
    }

    public void setCTBTR_1(String CTBTR_1) {
        this.CTBTR_1 = CTBTR_1;
    }

    public String getCTB_LOC1() {
        return CTB_LOC1;
    }

    public void setCTB_LOC1(String CTB_LOC1) {
        this.CTB_LOC1 = CTB_LOC1;
    }

    public Double getFIXING_1() {
        return FIXING_1;
    }

    public void setFIXING_1(Double FIXING_1) {
        this.FIXING_1 = FIXING_1;
    }

    public Double getCNV_PARITY() {
        return CNV_PARITY;
    }

    public void setCNV_PARITY(Double CNV_PARITY) {
        this.CNV_PARITY = CNV_PARITY;
    }

    public Double getPREMIUM() {
        return PREMIUM;
    }

    public void setPREMIUM(Double PREMIUM) {
        this.PREMIUM = PREMIUM;
    }

    public Double getCONV_FAC() {
        return CONV_FAC;
    }

    public void setCONV_FAC(Double CONV_FAC) {
        this.CONV_FAC = CONV_FAC;
    }

    public Double getYLD_TO_CLL() {
        return YLD_TO_CLL;
    }

    public void setYLD_TO_CLL(Double YLD_TO_CLL) {
        this.YLD_TO_CLL = YLD_TO_CLL;
    }

    public Double getYLD_TO_PUT() {
        return YLD_TO_PUT;
    }

    public void setYLD_TO_PUT(Double YLD_TO_PUT) {
        this.YLD_TO_PUT = YLD_TO_PUT;
    }

    public Double getSMP_MARGIN() {
        return SMP_MARGIN;
    }

    public void setSMP_MARGIN(Double SMP_MARGIN) {
        this.SMP_MARGIN = SMP_MARGIN;
    }

    public Double getDSC_MARGIN() {
        return DSC_MARGIN;
    }

    public void setDSC_MARGIN(Double DSC_MARGIN) {
        this.DSC_MARGIN = DSC_MARGIN;
    }

    public Double getIMP_VOLT() {
        return IMP_VOLT;
    }

    public void setIMP_VOLT(Double IMP_VOLT) {
        this.IMP_VOLT = IMP_VOLT;
    }

    public Double getIMP_VOLTA() {
        return IMP_VOLTA;
    }

    public void setIMP_VOLTA(Double IMP_VOLTA) {
        this.IMP_VOLTA = IMP_VOLTA;
    }

    public Double getIMP_VOLTB() {
        return IMP_VOLTB;
    }

    public void setIMP_VOLTB(Double IMP_VOLTB) {
        this.IMP_VOLTB = IMP_VOLTB;
    }

    public Double getOAS() {
        return OAS;
    }

    public void setOAS(Double OAS) {
        this.OAS = OAS;
    }

    public Double getREF_YIELD() {
        return REF_YIELD;
    }

    public void setREF_YIELD(Double REF_YIELD) {
        this.REF_YIELD = REF_YIELD;
    }

    public Double getDELTA() {
        return DELTA;
    }

    public void setDELTA(Double DELTA) {
        this.DELTA = DELTA;
    }

    public Double getIMP_REPO() {
        return IMP_REPO;
    }

    public void setIMP_REPO(Double IMP_REPO) {
        this.IMP_REPO = IMP_REPO;
    }

    public Double getSWAP_SPRD() {
        return SWAP_SPRD;
    }

    public void setSWAP_SPRD(Double SWAP_SPRD) {
        this.SWAP_SPRD = SWAP_SPRD;
    }

    public Double getSWAP_YLD() {
        return SWAP_YLD;
    }

    public void setSWAP_YLD(Double SWAP_YLD) {
        this.SWAP_YLD = SWAP_YLD;
    }

    public Double getASK_SPREAD() {
        return ASK_SPREAD;
    }

    public void setASK_SPREAD(Double ASK_SPREAD) {
        this.ASK_SPREAD = ASK_SPREAD;
    }

    public Double getAST_SWPSPD() {
        return AST_SWPSPD;
    }

    public void setAST_SWPSPD(Double AST_SWPSPD) {
        this.AST_SWPSPD = AST_SWPSPD;
    }

    public Double getBID_SPREAD() {
        return BID_SPREAD;
    }

    public void setBID_SPREAD(Double BID_SPREAD) {
        this.BID_SPREAD = BID_SPREAD;
    }

    public Double getBMK_SPD() {
        return BMK_SPD;
    }

    public void setBMK_SPD(Double BMK_SPD) {
        this.BMK_SPD = BMK_SPD;
    }

    public Double getBPV() {
        return BPV;
    }

    public void setBPV(Double BPV) {
        this.BPV = BPV;
    }

    public Double getDISC_RATE() {
        return DISC_RATE;
    }

    public void setDISC_RATE(Double DISC_RATE) {
        this.DISC_RATE = DISC_RATE;
    }

    public Double getDISC_MRGA() {
        return DISC_MRGA;
    }

    public void setDISC_MRGA(Double DISC_MRGA) {
        this.DISC_MRGA = DISC_MRGA;
    }

    public Double getDISC_MRGB() {
        return DISC_MRGB;
    }

    public void setDISC_MRGB(Double DISC_MRGB) {
        this.DISC_MRGB = DISC_MRGB;
    }

    public Double getISMA_B_YLD() {
        return ISMA_B_YLD;
    }

    public void setISMA_B_YLD(Double ISMA_B_YLD) {
        this.ISMA_B_YLD = ISMA_B_YLD;
    }

    public Double getISMA_A_YLD() {
        return ISMA_A_YLD;
    }

    public void setISMA_A_YLD(Double ISMA_A_YLD) {
        this.ISMA_A_YLD = ISMA_A_YLD;
    }

    public Double getMID_SPREAD() {
        return MID_SPREAD;
    }

    public void setMID_SPREAD(Double MID_SPREAD) {
        this.MID_SPREAD = MID_SPREAD;
    }

    public Double getMID_YLD_1() {
        return MID_YLD_1;
    }

    public void setMID_YLD_1(Double MID_YLD_1) {
        this.MID_YLD_1 = MID_YLD_1;
    }

    public Double getASTSWPSD_A() {
        return ASTSWPSD_A;
    }

    public void setASTSWPSD_A(Double ASTSWPSD_A) {
        this.ASTSWPSD_A = ASTSWPSD_A;
    }

    public Double getASTSWPSD_B() {
        return ASTSWPSD_B;
    }

    public void setASTSWPSD_B(Double ASTSWPSD_B) {
        this.ASTSWPSD_B = ASTSWPSD_B;
    }

    public Long getFIX_DATE() {
        return FIX_DATE;
    }

    public void setFIX_DATE(Long FIX_DATE) {
        this.FIX_DATE = FIX_DATE;
    }

    public Double getCDS_BASIS() {
        return CDS_BASIS;
    }

    public void setCDS_BASIS(Double CDS_BASIS) {
        this.CDS_BASIS = CDS_BASIS;
    }

    public Double getCDS_DV01() {
        return CDS_DV01;
    }

    public void setCDS_DV01(Double CDS_DV01) {
        this.CDS_DV01 = CDS_DV01;
    }

    public Double getBEVEN_INF() {
        return BEVEN_INF;
    }

    public void setBEVEN_INF(Double BEVEN_INF) {
        this.BEVEN_INF = BEVEN_INF;
    }

    public Double getREAL_YLDA() {
        return REAL_YLDA;
    }

    public void setREAL_YLDA(Double REAL_YLDA) {
        this.REAL_YLDA = REAL_YLDA;
    }

    public Double getREAL_YLDB() {
        return REAL_YLDB;
    }

    public void setREAL_YLDB(Double REAL_YLDB) {
        this.REAL_YLDB = REAL_YLDB;
    }

    public Double getZSPREAD() {
        return ZSPREAD;
    }

    public void setZSPREAD(Double ZSPREAD) {
        this.ZSPREAD = ZSPREAD;
    }

    public Double getBMK_SPDA() {
        return BMK_SPDA;
    }

    public void setBMK_SPDA(Double BMK_SPDA) {
        this.BMK_SPDA = BMK_SPDA;
    }

    public Double getBMK_SPDB() {
        return BMK_SPDB;
    }

    public void setBMK_SPDB(Double BMK_SPDB) {
        this.BMK_SPDB = BMK_SPDB;
    }

    public Double getBMK_YIELD() {
        return BMK_YIELD;
    }

    public void setBMK_YIELD(Double BMK_YIELD) {
        this.BMK_YIELD = BMK_YIELD;
    }

    public Double getBP_VOLT() {
        return BP_VOLT;
    }

    public void setBP_VOLT(Double BP_VOLT) {
        this.BP_VOLT = BP_VOLT;
    }

    public Double getCAP_PREM() {
        return CAP_PREM;
    }

    public void setCAP_PREM(Double CAP_PREM) {
        this.CAP_PREM = CAP_PREM;
    }

    public Double getCNV_EDGE() {
        return CNV_EDGE;
    }

    public void setCNV_EDGE(Double CNV_EDGE) {
        this.CNV_EDGE = CNV_EDGE;
    }

    public Double getCONVX_BIAS() {
        return CONVX_BIAS;
    }

    public void setCONVX_BIAS(Double CONVX_BIAS) {
        this.CONVX_BIAS = CONVX_BIAS;
    }

    public Double getDISC_FACTR() {
        return DISC_FACTR;
    }

    public void setDISC_FACTR(Double DISC_FACTR) {
        this.DISC_FACTR = DISC_FACTR;
    }

    public Double getFAIR_PRICE() {
        return FAIR_PRICE;
    }

    public void setFAIR_PRICE(Double FAIR_PRICE) {
        this.FAIR_PRICE = FAIR_PRICE;
    }

    public Double getFC_AVSWPTS() {
        return FC_AVSWPTS;
    }

    public void setFC_AVSWPTS(Double FC_AVSWPTS) {
        this.FC_AVSWPTS = FC_AVSWPTS;
    }

    public Double getFCAST_MAX() {
        return FCAST_MAX;
    }

    public void setFCAST_MAX(Double FCAST_MAX) {
        this.FCAST_MAX = FCAST_MAX;
    }

    public Double getFCAST_MEAN() {
        return FCAST_MEAN;
    }

    public void setFCAST_MEAN(Double FCAST_MEAN) {
        this.FCAST_MEAN = FCAST_MEAN;
    }

    public Double getFCAST_MED() {
        return FCAST_MED;
    }

    public void setFCAST_MED(Double FCAST_MED) {
        this.FCAST_MED = FCAST_MED;
    }

    public Double getFCAST_MIN() {
        return FCAST_MIN;
    }

    public void setFCAST_MIN(Double FCAST_MIN) {
        this.FCAST_MIN = FCAST_MIN;
    }

    public Double getFCAST_STDV() {
        return FCAST_STDV;
    }

    public void setFCAST_STDV(Double FCAST_STDV) {
        this.FCAST_STDV = FCAST_STDV;
    }

    public Double getFLOOR_PREM() {
        return FLOOR_PREM;
    }

    public void setFLOOR_PREM(Double FLOOR_PREM) {
        this.FLOOR_PREM = FLOOR_PREM;
    }

    public Double getFUT_BASIS() {
        return FUT_BASIS;
    }

    public void setFUT_BASIS(Double FUT_BASIS) {
        this.FUT_BASIS = FUT_BASIS;
    }

    public Double getFUT_RISK() {
        return FUT_RISK;
    }

    public void setFUT_RISK(Double FUT_RISK) {
        this.FUT_RISK = FUT_RISK;
    }

    public Double getFWD_OR_ASK() {
        return FWD_OR_ASK;
    }

    public void setFWD_OR_ASK(Double FWD_OR_ASK) {
        this.FWD_OR_ASK = FWD_OR_ASK;
    }

    public Double getFWD_OR_BID() {
        return FWD_OR_BID;
    }

    public void setFWD_OR_BID(Double FWD_OR_BID) {
        this.FWD_OR_BID = FWD_OR_BID;
    }

    public Double getIMP_YIELD() {
        return IMP_YIELD;
    }

    public void setIMP_YIELD(Double IMP_YIELD) {
        this.IMP_YIELD = IMP_YIELD;
    }

    public Double getINDEX_SKEW() {
        return INDEX_SKEW;
    }

    public void setINDEX_SKEW(Double INDEX_SKEW) {
        this.INDEX_SKEW = INDEX_SKEW;
    }

    public Double getMEAN_REV() {
        return MEAN_REV;
    }

    public void setMEAN_REV(Double MEAN_REV) {
        this.MEAN_REV = MEAN_REV;
    }

    public Long getRESR_VOL() {
        return RESR_VOL;
    }

    public void setRESR_VOL(Long RESR_VOL) {
        this.RESR_VOL = RESR_VOL;
    }

    public Double getSH_RT_VOLT() {
        return SH_RT_VOLT;
    }

    public void setSH_RT_VOLT(Double SH_RT_VOLT) {
        this.SH_RT_VOLT = SH_RT_VOLT;
    }

    public Double getSWAP_PREM() {
        return SWAP_PREM;
    }

    public void setSWAP_PREM(Double SWAP_PREM) {
        this.SWAP_PREM = SWAP_PREM;
    }

    public Double getSWAP_SPRDA() {
        return SWAP_SPRDA;
    }

    public void setSWAP_SPRDA(Double SWAP_SPRDA) {
        this.SWAP_SPRDA = SWAP_SPRDA;
    }

    public Double getSWAP_SPRDB() {
        return SWAP_SPRDB;
    }

    public void setSWAP_SPRDB(Double SWAP_SPRDB) {
        this.SWAP_SPRDB = SWAP_SPRDB;
    }

    public Double getYLDTOBEST() {
        return YLDTOBEST;
    }

    public void setYLDTOBEST(Double YLDTOBEST) {
        this.YLDTOBEST = YLDTOBEST;
    }

    public Double getYLDTOWORST() {
        return YLDTOWORST;
    }

    public void setYLDTOWORST(Double YLDTOWORST) {
        this.YLDTOWORST = YLDTOWORST;
    }

    public Double getLAST_QUOTE() {
        return LAST_QUOTE;
    }

    public void setLAST_QUOTE(Double LAST_QUOTE) {
        this.LAST_QUOTE = LAST_QUOTE;
    }

    public Double getDISC_BID1() {
        return DISC_BID1;
    }

    public void setDISC_BID1(Double DISC_BID1) {
        this.DISC_BID1 = DISC_BID1;
    }

    public Double getDISC_ASK1() {
        return DISC_ASK1;
    }

    public void setDISC_ASK1(Double DISC_ASK1) {
        this.DISC_ASK1 = DISC_ASK1;
    }

    public Double getBOND_FLOOR() {
        return BOND_FLOOR;
    }

    public void setBOND_FLOOR(Double BOND_FLOOR) {
        this.BOND_FLOOR = BOND_FLOOR;
    }

    public Double getDURATION() {
        return DURATION;
    }

    public void setDURATION(Double DURATION) {
        this.DURATION = DURATION;
    }

    public Double getTHETA() {
        return THETA;
    }

    public void setTHETA(Double THETA) {
        this.THETA = THETA;
    }

    public Double getGAMMA() {
        return GAMMA;
    }

    public void setGAMMA(Double GAMMA) {
        this.GAMMA = GAMMA;
    }

    public Double getCONVEXITY() {
        return CONVEXITY;
    }

    public void setCONVEXITY(Double CONVEXITY) {
        this.CONVEXITY = CONVEXITY;
    }

    public Double getIMP_CORR() {
        return IMP_CORR;
    }

    public void setIMP_CORR(Double IMP_CORR) {
        this.IMP_CORR = IMP_CORR;
    }

    public Double getRUN_SPRD() {
        return RUN_SPRD;
    }

    public void setRUN_SPRD(Double RUN_SPRD) {
        this.RUN_SPRD = RUN_SPRD;
    }

    public Double getBASE_CORR() {
        return BASE_CORR;
    }

    public void setBASE_CORR(Double BASE_CORR) {
        this.BASE_CORR = BASE_CORR;
    }

    public Double getPAR_YLD() {
        return PAR_YLD;
    }

    public void setPAR_YLD(Double PAR_YLD) {
        this.PAR_YLD = PAR_YLD;
    }

    public Double getZERO_YLD() {
        return ZERO_YLD;
    }

    public void setZERO_YLD(Double ZERO_YLD) {
        this.ZERO_YLD = ZERO_YLD;
    }

    public Double getPRC_VOLTY() {
        return PRC_VOLTY;
    }

    public void setPRC_VOLTY(Double PRC_VOLTY) {
        this.PRC_VOLTY = PRC_VOLTY;
    }

    public Long getChangeTimeStamp() {
        return ChangeTimeStamp;
    }

    public void setChangeTimeStamp(Long ChangeTimeStamp) {
        this.ChangeTimeStamp = ChangeTimeStamp;
    }

    public Long getChangeSequence() {
        return ChangeSequence;
    }

    public void setChangeSequence(Long ChangeSequence) {
        this.ChangeSequence = ChangeSequence;
    }

    public Integer getTAQFILTER() {
        return TAQFILTER;
    }

    public void setTAQFILTER(Integer TAQFILTER) {
        this.TAQFILTER = TAQFILTER;
    }

    public Double getRECOV_RATE() {
        return RECOV_RATE;
    }

    public void setRECOV_RATE(Double RECOV_RATE) {
        this.RECOV_RATE = RECOV_RATE;
    }

    public Double getDEFLT_PROB() {
        return DEFLT_PROB;
    }

    public void setDEFLT_PROB(Double DEFLT_PROB) {
        this.DEFLT_PROB = DEFLT_PROB;
    }

    public Double getOAS_BID() {
        return OAS_BID;
    }

    public void setOAS_BID(Double OAS_BID) {
        this.OAS_BID = OAS_BID;
    }

    public Double getOAS_ASK() {
        return OAS_ASK;
    }

    public void setOAS_ASK(Double OAS_ASK) {
        this.OAS_ASK = OAS_ASK;
    }

    public Double getYTM_BID() {
        return YTM_BID;
    }

    public void setYTM_BID(Double YTM_BID) {
        this.YTM_BID = YTM_BID;
    }

    public Double getYTM_ASK() {
        return YTM_ASK;
    }

    public void setYTM_ASK(Double YTM_ASK) {
        this.YTM_ASK = YTM_ASK;
    }

    public Double getPV01() {
        return PV01;
    }

    public void setPV01(Double PV01) {
        this.PV01 = PV01;
    }

    public Double getUPLIMIT() {
        return UPLIMIT;
    }

    public void setUPLIMIT(Double UPLIMIT) {
        this.UPLIMIT = UPLIMIT;
    }

    public Double getLOLIMIT() {
        return LOLIMIT;
    }

    public void setLOLIMIT(Double LOLIMIT) {
        this.LOLIMIT = LOLIMIT;
    }

    public Double getCMP_YLD_B() {
        return CMP_YLD_B;
    }

    public void setCMP_YLD_B(Double CMP_YLD_B) {
        this.CMP_YLD_B = CMP_YLD_B;
    }

    public Double getCMP_YLD_A() {
        return CMP_YLD_A;
    }

    public void setCMP_YLD_A(Double CMP_YLD_A) {
        this.CMP_YLD_A = CMP_YLD_A;
    }

    public Double getASP1M() {
        return ASP1M;
    }

    public void setASP1M(Double ASP1M) {
        this.ASP1M = ASP1M;
    }

    public Double getASP3M() {
        return ASP3M;
    }

    public void setASP3M(Double ASP3M) {
        this.ASP3M = ASP3M;
    }

    public Double getASP6M() {
        return ASP6M;
    }

    public void setASP6M(Double ASP6M) {
        this.ASP6M = ASP6M;
    }

    public Double getCPN_FIXED1() {
        return CPN_FIXED1;
    }

    public void setCPN_FIXED1(Double CPN_FIXED1) {
        this.CPN_FIXED1 = CPN_FIXED1;
    }

    public Double getTHEO_SPD_B() {
        return THEO_SPD_B;
    }

    public void setTHEO_SPD_B(Double THEO_SPD_B) {
        this.THEO_SPD_B = THEO_SPD_B;
    }

    public Double getTHEO_SPD_M() {
        return THEO_SPD_M;
    }

    public void setTHEO_SPD_M(Double THEO_SPD_M) {
        this.THEO_SPD_M = THEO_SPD_M;
    }

    public Double getTHEO_SPD_A() {
        return THEO_SPD_A;
    }

    public void setTHEO_SPD_A(Double THEO_SPD_A) {
        this.THEO_SPD_A = THEO_SPD_A;
    }

    public Double getTHEO_PRC_B() {
        return THEO_PRC_B;
    }

    public void setTHEO_PRC_B(Double THEO_PRC_B) {
        this.THEO_PRC_B = THEO_PRC_B;
    }

    public Double getTHEO_PRC_M() {
        return THEO_PRC_M;
    }

    public void setTHEO_PRC_M(Double THEO_PRC_M) {
        this.THEO_PRC_M = THEO_PRC_M;
    }

    public Double getTHEO_PRC_A() {
        return THEO_PRC_A;
    }

    public void setTHEO_PRC_A(Double THEO_PRC_A) {
        this.THEO_PRC_A = THEO_PRC_A;
    }

    public Double getCARRY_COST() {
        return CARRY_COST;
    }

    public void setCARRY_COST(Double CARRY_COST) {
        this.CARRY_COST = CARRY_COST;
    }

    public Double getROLL_DOWN() {
        return ROLL_DOWN;
    }

    public void setROLL_DOWN(Double ROLL_DOWN) {
        this.ROLL_DOWN = ROLL_DOWN;
    }

    public Double getCRD_TOTAL() {
        return CRD_TOTAL;
    }

    public void setCRD_TOTAL(Double CRD_TOTAL) {
        this.CRD_TOTAL = CRD_TOTAL;
    }

    public Double getFWD_RATE() {
        return FWD_RATE;
    }

    public void setFWD_RATE(Double FWD_RATE) {
        this.FWD_RATE = FWD_RATE;
    }

    public Double getCM_YLD() {
        return CM_YLD;
    }

    public void setCM_YLD(Double CM_YLD) {
        this.CM_YLD = CM_YLD;
    }

    public Long getB_DLR_CNT() {
        return B_DLR_CNT;
    }

    public void setB_DLR_CNT(Long B_DLR_CNT) {
        this.B_DLR_CNT = B_DLR_CNT;
    }

    public Long getA_DLR_CNT() {
        return A_DLR_CNT;
    }

    public void setA_DLR_CNT(Long A_DLR_CNT) {
        this.A_DLR_CNT = A_DLR_CNT;
    }

    public Double getSOV_SPREAD() {
        return SOV_SPREAD;
    }

    public void setSOV_SPREAD(Double SOV_SPREAD) {
        this.SOV_SPREAD = SOV_SPREAD;
    }

    public Long getAC_BID_ORD() {
        return AC_BID_ORD;
    }

    public void setAC_BID_ORD(Long AC_BID_ORD) {
        this.AC_BID_ORD = AC_BID_ORD;
    }

    public Long getAC_ASK_ORD() {
        return AC_ASK_ORD;
    }

    public void setAC_ASK_ORD(Long AC_ASK_ORD) {
        this.AC_ASK_ORD = AC_ASK_ORD;
    }

    public Long getAC_BORD_SZ() {
        return AC_BORD_SZ;
    }

    public void setAC_BORD_SZ(Long AC_BORD_SZ) {
        this.AC_BORD_SZ = AC_BORD_SZ;
    }

    public Long getAC_AORD_SZ() {
        return AC_AORD_SZ;
    }

    public void setAC_AORD_SZ(Long AC_AORD_SZ) {
        this.AC_AORD_SZ = AC_AORD_SZ;
    }

    public String getLIMIT_INDQ() {
        return LIMIT_INDQ;
    }

    public void setLIMIT_INDQ(String LIMIT_INDQ) {
        this.LIMIT_INDQ = LIMIT_INDQ;
    }

    public String getSH_SAL_RES() {
        return SH_SAL_RES;
    }

    public void setSH_SAL_RES(String SH_SAL_RES) {
        this.SH_SAL_RES = SH_SAL_RES;
    }

    public String getTRD_STATUS() {
        return TRD_STATUS;
    }

    public void setTRD_STATUS(String TRD_STATUS) {
        this.TRD_STATUS = TRD_STATUS;
    }

    public String getHALT_RSN() {
        return HALT_RSN;
    }

    public void setHALT_RSN(String HALT_RSN) {
        this.HALT_RSN = HALT_RSN;
    }

    public Double getNETCHNG_1() {
        return NETCHNG_1;
    }

    public void setNETCHNG_1(Double NETCHNG_1) {
        this.NETCHNG_1 = NETCHNG_1;
    }

    public Double getEFF_DURTN() {
        return EFF_DURTN;
    }

    public void setEFF_DURTN(Double EFF_DURTN) {
        this.EFF_DURTN = EFF_DURTN;
    }

    public Double getEFF_CONVX() {
        return EFF_CONVX;
    }

    public void setEFF_CONVX(Double EFF_CONVX) {
        this.EFF_CONVX = EFF_CONVX;
    }

    public Double getESPRD_TSRY() {
        return ESPRD_TSRY;
    }

    public void setESPRD_TSRY(Double ESPRD_TSRY) {
        this.ESPRD_TSRY = ESPRD_TSRY;
    }

    public Double getFV_SPREAD() {
        return FV_SPREAD;
    }

    public void setFV_SPREAD(Double FV_SPREAD) {
        this.FV_SPREAD = FV_SPREAD;
    }

    public Double getFV_YIELD() {
        return FV_YIELD;
    }

    public void setFV_YIELD(Double FV_YIELD) {
        this.FV_YIELD = FV_YIELD;
    }

    public Double getFV_SCORE() {
        return FV_SCORE;
    }

    public void setFV_SCORE(Double FV_SCORE) {
        this.FV_SCORE = FV_SCORE;
    }

    public Double getFV_SCR_DEV() {
        return FV_SCR_DEV;
    }

    public void setFV_SCR_DEV(Double FV_SCR_DEV) {
        this.FV_SCR_DEV = FV_SCR_DEV;
    }

    public Double getFV_DV01() {
        return FV_DV01;
    }

    public void setFV_DV01(Double FV_DV01) {
        this.FV_DV01 = FV_DV01;
    }

    public Double getTHEO_PRC() {
        return THEO_PRC;
    }

    public void setTHEO_PRC(Double THEO_PRC) {
        this.THEO_PRC = THEO_PRC;
    }

    public Double getGRS_BASIS() {
        return GRS_BASIS;
    }

    public void setGRS_BASIS(Double GRS_BASIS) {
        this.GRS_BASIS = GRS_BASIS;
    }

    public Double getFWD_RISK() {
        return FWD_RISK;
    }

    public void setFWD_RISK(Double FWD_RISK) {
        this.FWD_RISK = FWD_RISK;
    }

    public Double getHEDGERATIO() {
        return HEDGERATIO;
    }

    public void setHEDGERATIO(Double HEDGERATIO) {
        this.HEDGERATIO = HEDGERATIO;
    }

    public Double getNET_BASIS() {
        return NET_BASIS;
    }

    public void setNET_BASIS(Double NET_BASIS) {
        this.NET_BASIS = NET_BASIS;
    }

    public Double getINV_PRC() {
        return INV_PRC;
    }

    public void setINV_PRC(Double INV_PRC) {
        this.INV_PRC = INV_PRC;
    }

    public Double getINV_SPD() {
        return INV_SPD;
    }

    public void setINV_SPD(Double INV_SPD) {
        this.INV_SPD = INV_SPD;
    }

    public Double getIMB_PR_FR() {
        return IMB_PR_FR;
    }

    public void setIMB_PR_FR(Double IMB_PR_FR) {
        this.IMB_PR_FR = IMB_PR_FR;
    }

    public Double getIMB_PR_NR() {
        return IMB_PR_NR;
    }

    public void setIMB_PR_NR(Double IMB_PR_NR) {
        this.IMB_PR_NR = IMB_PR_NR;
    }

    public Double getIMB_PR_REF() {
        return IMB_PR_REF;
    }

    public void setIMB_PR_REF(Double IMB_PR_REF) {
        this.IMB_PR_REF = IMB_PR_REF;
    }

    public Long getIMB_PR_SH() {
        return IMB_PR_SH;
    }

    public void setIMB_PR_SH(Long IMB_PR_SH) {
        this.IMB_PR_SH = IMB_PR_SH;
    }

    public Long getIMB_SH() {
        return IMB_SH;
    }

    public void setIMB_SH(Long IMB_SH) {
        this.IMB_SH = IMB_SH;
    }

    public String getIMB_SIDE() {
        return IMB_SIDE;
    }

    public void setIMB_SIDE(String IMB_SIDE) {
        this.IMB_SIDE = IMB_SIDE;
    }

    public String getIMB_VA_IND() {
        return IMB_VA_IND;
    }

    public void setIMB_VA_IND(String IMB_VA_IND) {
        this.IMB_VA_IND = IMB_VA_IND;
    }

    public String getIMB_ACT_TP() {
        return IMB_ACT_TP;
    }

    public void setIMB_ACT_TP(String IMB_ACT_TP) {
        this.IMB_ACT_TP = IMB_ACT_TP;
    }

    public Double getFCAST_HIGH() {
        return FCAST_HIGH;
    }

    public void setFCAST_HIGH(Double FCAST_HIGH) {
        this.FCAST_HIGH = FCAST_HIGH;
    }

    public Double getFCAST_LOW() {
        return FCAST_LOW;
    }

    public void setFCAST_LOW(Double FCAST_LOW) {
        this.FCAST_LOW = FCAST_LOW;
    }

    public Double getFCAST_MEDN() {
        return FCAST_MEDN;
    }

    public void setFCAST_MEDN(Double FCAST_MEDN) {
        this.FCAST_MEDN = FCAST_MEDN;
    }

    public Double getASK_FWDORT() {
        return ASK_FWDORT;
    }

    public void setASK_FWDORT(Double ASK_FWDORT) {
        this.ASK_FWDORT = ASK_FWDORT;
    }

    public Double getBID_FWDORT() {
        return BID_FWDORT;
    }

    public void setBID_FWDORT(Double BID_FWDORT) {
        this.BID_FWDORT = BID_FWDORT;
    }

    public Double getPAR_YLD1() {
        return PAR_YLD1;
    }

    public void setPAR_YLD1(Double PAR_YLD1) {
        this.PAR_YLD1 = PAR_YLD1;
    }

    public Double getRUN_SPREAD() {
        return RUN_SPREAD;
    }

    public void setRUN_SPREAD(Double RUN_SPREAD) {
        this.RUN_SPREAD = RUN_SPREAD;
    }

    public Double getZERO_YLD1() {
        return ZERO_YLD1;
    }

    public void setZERO_YLD1(Double ZERO_YLD1) {
        this.ZERO_YLD1 = ZERO_YLD1;
    }

    public Double getBMK_SPDASK() {
        return BMK_SPDASK;
    }

    public void setBMK_SPDASK(Double BMK_SPDASK) {
        this.BMK_SPDASK = BMK_SPDASK;
    }

    public Double getBMK_SPDBID() {
        return BMK_SPDBID;
    }

    public void setBMK_SPDBID(Double BMK_SPDBID) {
        this.BMK_SPDBID = BMK_SPDBID;
    }

    public Double getBOND_FLR() {
        return BOND_FLR;
    }

    public void setBOND_FLR(Double BOND_FLR) {
        this.BOND_FLR = BOND_FLR;
    }

    public Double getCNV_EDGE1() {
        return CNV_EDGE1;
    }

    public void setCNV_EDGE1(Double CNV_EDGE1) {
        this.CNV_EDGE1 = CNV_EDGE1;
    }

    public Double getYLDBST() {
        return YLDBST;
    }

    public void setYLDBST(Double YLDBST) {
        this.YLDBST = YLDBST;
    }

    public Double getYLDWST() {
        return YLDWST;
    }

    public void setYLDWST(Double YLDWST) {
        this.YLDWST = YLDWST;
    }

    public Double getOPEN_PRC() {
        return OPEN_PRC;
    }

    public void setOPEN_PRC(Double OPEN_PRC) {
        this.OPEN_PRC = OPEN_PRC;
    }

    public Double getHIGH_1() {
        return HIGH_1;
    }

    public void setHIGH_1(Double HIGH_1) {
        this.HIGH_1 = HIGH_1;
    }

    public Double getLOW_1() {
        return LOW_1;
    }

    public void setLOW_1(Double LOW_1) {
        this.LOW_1 = LOW_1;
    }

    public Double getOPEN_YLD() {
        return OPEN_YLD;
    }

    public void setOPEN_YLD(Double OPEN_YLD) {
        this.OPEN_YLD = OPEN_YLD;
    }

    public Double getHIGH_YLD() {
        return HIGH_YLD;
    }

    public void setHIGH_YLD(Double HIGH_YLD) {
        this.HIGH_YLD = HIGH_YLD;
    }

    public Double getLOW_YLD() {
        return LOW_YLD;
    }

    public void setLOW_YLD(Double LOW_YLD) {
        this.LOW_YLD = LOW_YLD;
    }

    public Double getBENCH_PRC() {
        return BENCH_PRC;
    }

    public void setBENCH_PRC(Double BENCH_PRC) {
        this.BENCH_PRC = BENCH_PRC;
    }

    public String getBKGD_REF() {
        return BKGD_REF;
    }

    public void setBKGD_REF(String BKGD_REF) {
        this.BKGD_REF = BKGD_REF;
    }

    public Double getNRG_CRACK() {
        return NRG_CRACK;
    }

    public void setNRG_CRACK(Double NRG_CRACK) {
        this.NRG_CRACK = NRG_CRACK;
    }

    public Double getNRG_FRGHT() {
        return NRG_FRGHT;
    }

    public void setNRG_FRGHT(Double NRG_FRGHT) {
        this.NRG_FRGHT = NRG_FRGHT;
    }

    public Double getNRG_TOP() {
        return NRG_TOP;
    }

    public void setNRG_TOP(Double NRG_TOP) {
        this.NRG_TOP = NRG_TOP;
    }

    public Double getTRDVOL_1() {
        return TRDVOL_1;
    }

    public void setTRDVOL_1(Double TRDVOL_1) {
        this.TRDVOL_1 = TRDVOL_1;
    }

    public Double getYIELD() {
        return YIELD;
    }

    public void setYIELD(Double YIELD) {
        this.YIELD = YIELD;
    }

    public String getBID_TICK_1() {
        return BID_TICK_1;
    }

    public void setBID_TICK_1(String BID_TICK_1) {
        this.BID_TICK_1 = BID_TICK_1;
    }

    public Double getINT_BASIS() {
        return INT_BASIS;
    }

    public void setINT_BASIS(Double INT_BASIS) {
        this.INT_BASIS = INT_BASIS;
    }

    public Double getINT_CDS() {
        return INT_CDS;
    }

    public void setINT_CDS(Double INT_CDS) {
        this.INT_CDS = INT_CDS;
    }

    public Double getMOD_DURTN() {
        return MOD_DURTN;
    }

    public void setMOD_DURTN(Double MOD_DURTN) {
        this.MOD_DURTN = MOD_DURTN;
    }

    public Double getSWP_POINT() {
        return SWP_POINT;
    }

    public void setSWP_POINT(Double SWP_POINT) {
        this.SWP_POINT = SWP_POINT;
    }

    public Double getCLEAN_PRC() {
        return CLEAN_PRC;
    }

    public void setCLEAN_PRC(Double CLEAN_PRC) {
        this.CLEAN_PRC = CLEAN_PRC;
    }

    public String getISIN_CODE() {
        return ISIN_CODE;
    }

    public void setISIN_CODE(String ISIN_CODE) {
        this.ISIN_CODE = ISIN_CODE;
    }

    public String getMIC_CODE() {
        return MIC_CODE;
    }

    public void setMIC_CODE(String MIC_CODE) {
        this.MIC_CODE = MIC_CODE;
    }

    public String getMIFIR_ID() {
        return MIFIR_ID;
    }

    public void setMIFIR_ID(String MIFIR_ID) {
        this.MIFIR_ID = MIFIR_ID;
    }

    public String getMIFIR_U_AS() {
        return MIFIR_U_AS;
    }

    public void setMIFIR_U_AS(String MIFIR_U_AS) {
        this.MIFIR_U_AS = MIFIR_U_AS;
    }

    public String getMIFIR_C_TP() {
        return MIFIR_C_TP;
    }

    public void setMIFIR_C_TP(String MIFIR_C_TP) {
        this.MIFIR_C_TP = MIFIR_C_TP;
    }

    public Long getSOURCE_DATETIME_EXT() {
        return SOURCE_DATETIME_EXT;
    }

    public void setSOURCE_DATETIME_EXT(Long SOURCE_DATETIME_EXT) {
        this.SOURCE_DATETIME_EXT = SOURCE_DATETIME_EXT;
    }

    public Double getQUOTE_VAL() {
        return QUOTE_VAL;
    }

    public void setQUOTE_VAL(Double QUOTE_VAL) {
        this.QUOTE_VAL = QUOTE_VAL;
    }

    public String getQTE_ID() {
        return QTE_ID;
    }

    public void setQTE_ID(String QTE_ID) {
        this.QTE_ID = QTE_ID;
    }

    public Double getQUOTE_SIZE() {
        return QUOTE_SIZE;
    }

    public void setQUOTE_SIZE(Double QUOTE_SIZE) {
        this.QUOTE_SIZE = QUOTE_SIZE;
    }

    public String getISIN_CD_D() {
        return ISIN_CD_D;
    }

    public void setISIN_CD_D(String ISIN_CD_D) {
        this.ISIN_CD_D = ISIN_CD_D;
    }

    public Double getTRTN_PRICE() {
        return TRTN_PRICE;
    }

    public void setTRTN_PRICE(Double TRTN_PRICE) {
        this.TRTN_PRICE = TRTN_PRICE;
    }

    public Double getCNV_PREM() {
        return CNV_PREM;
    }

    public void setCNV_PREM(Double CNV_PREM) {
        this.CNV_PREM = CNV_PREM;
    }

    public Double getCNV_RATIO() {
        return CNV_RATIO;
    }

    public void setCNV_RATIO(Double CNV_RATIO) {
        this.CNV_RATIO = CNV_RATIO;
    }

    public Double getCURR_BID() {
        return CURR_BID;
    }

    public void setCURR_BID(Double CURR_BID) {
        this.CURR_BID = CURR_BID;
    }

    public Double getCURR_ASK() {
        return CURR_ASK;
    }

    public void setCURR_ASK(Double CURR_ASK) {
        this.CURR_ASK = CURR_ASK;
    }

    public String getEXCH_DATE() {
        return EXCH_DATE;
    }

    public void setEXCH_DATE(String EXCH_DATE) {
        this.EXCH_DATE = EXCH_DATE;
    }

    public String getEXCH_TIME() {
        return EXCH_TIME;
    }

    public void setEXCH_TIME(String EXCH_TIME) {
        this.EXCH_TIME = EXCH_TIME;
    }

    public String getSUM_ACTION_1() {
        return SUM_ACTION_1;
    }

    public void setSUM_ACTION_1(String SUM_ACTION_1) {
        this.SUM_ACTION_1 = SUM_ACTION_1;
    }

    public String getSUM_ACTION_2() {
        return SUM_ACTION_2;
    }

    public void setSUM_ACTION_2(String SUM_ACTION_2) {
        this.SUM_ACTION_2 = SUM_ACTION_2;
    }

    public String getSUM_ACTION_3() {
        return SUM_ACTION_3;
    }

    public void setSUM_ACTION_3(String SUM_ACTION_3) {
        this.SUM_ACTION_3 = SUM_ACTION_3;
    }

    public String getSUM_ACTION_4() {
        return SUM_ACTION_4;
    }

    public void setSUM_ACTION_4(String SUM_ACTION_4) {
        this.SUM_ACTION_4 = SUM_ACTION_4;
    }

    public String getSUM_ACTION_5() {
        return SUM_ACTION_5;
    }

    public void setSUM_ACTION_5(String SUM_ACTION_5) {
        this.SUM_ACTION_5 = SUM_ACTION_5;
    }

    public Double getRHO() {
        return RHO;
    }

    public void setRHO(Double RHO) {
        this.RHO = RHO;
    }

    public Double getVEGA() {
        return VEGA;
    }

    public void setVEGA(Double VEGA) {
        this.VEGA = VEGA;
    }

    public String getQualifiers() {
        return qualifiers;
    }

    public void setQualifiers(String qualifiers) {
        this.qualifiers = qualifiers;
    }

    public String getUser_qualifiers() {
        return user_qualifiers;
    }

    public void setUser_qualifiers(String user_qualifiers) {
        this.user_qualifiers = user_qualifiers;
    }

    public String getMessageTimestamp_st() {
        return messageTimestamp_st;
    }

    public void setMessageTimestamp_st(String messageTimestamp_st) {
        this.messageTimestamp_st = messageTimestamp_st;
    }

    public String getExecutionTime_st() {
        return executionTime_st;
    }

    public void setExecutionTime_st(String executionTime_st) {
        this.executionTime_st = executionTime_st;
    }

    public String getCOLLECT_DATETIME_st() {
        return COLLECT_DATETIME_st;
    }

    public void setCOLLECT_DATETIME_st(String COLLECT_DATETIME_st) {
        this.COLLECT_DATETIME_st = COLLECT_DATETIME_st;
    }

    public String getSOURCE_DATETIME_st() {
        return SOURCE_DATETIME_st;
    }

    public void setSOURCE_DATETIME_st(String SOURCE_DATETIME_st) {
        this.SOURCE_DATETIME_st = SOURCE_DATETIME_st;
    }

    public String getChangeTimeStamp_st() {
        return ChangeTimeStamp_st;
    }

    public void setChangeTimeStamp_st(String ChangeTimeStamp_st) {
        this.ChangeTimeStamp_st = ChangeTimeStamp_st;
    }

    public java.time.LocalDateTime getMessageTimestamp_dt() {
        return messageTimestamp_dt;
    }

    public void setMessageTimestamp_dt(java.time.LocalDateTime messageTimestamp_dt) {
        this.messageTimestamp_dt = messageTimestamp_dt;
    }

    public java.time.LocalDateTime getExecutionTime_dt() {
        return executionTime_dt;
    }

    public void setExecutionTime_dt(java.time.LocalDateTime executionTime_dt) {
        this.executionTime_dt = executionTime_dt;
    }

    public java.time.LocalDateTime getCOLLECT_DATETIME_dt() {
        return COLLECT_DATETIME_dt;
    }

    public void setCOLLECT_DATETIME_dt(java.time.LocalDateTime COLLECT_DATETIME_dt) {
        this.COLLECT_DATETIME_dt = COLLECT_DATETIME_dt;
    }

    public java.time.LocalDateTime getSOURCE_DATETIME_dt() {
        return SOURCE_DATETIME_dt;
    }

    public void setSOURCE_DATETIME_dt(java.time.LocalDateTime SOURCE_DATETIME_dt) {
        this.SOURCE_DATETIME_dt = SOURCE_DATETIME_dt;
    }

    public java.time.LocalDateTime getChangeTimeStamp_dt() {
        return ChangeTimeStamp_dt;
    }

    public void setChangeTimeStamp_dt(java.time.LocalDateTime ChangeTimeStamp_dt) {
        this.ChangeTimeStamp_dt = ChangeTimeStamp_dt;
    }

    public Integer getMessageTimestamp_ns() {
        return messageTimestamp_ns;
    }

    public void setMessageTimestamp_ns(Integer messageTimestamp_ns) {
        this.messageTimestamp_ns = messageTimestamp_ns;
    }

    public Integer getExecutionTime_ns() {
        return executionTime_ns;
    }

    public void setExecutionTime_ns(Integer executionTime_ns) {
        this.executionTime_ns = executionTime_ns;
    }

    public Integer getCOLLECT_DATETIME_ns() {
        return COLLECT_DATETIME_ns;
    }

    public void setCOLLECT_DATETIME_ns(Integer COLLECT_DATETIME_ns) {
        this.COLLECT_DATETIME_ns = COLLECT_DATETIME_ns;
    }

    public Integer getSOURCE_DATETIME_ns() {
        return SOURCE_DATETIME_ns;
    }

    public void setSOURCE_DATETIME_ns(Integer SOURCE_DATETIME_ns) {
        this.SOURCE_DATETIME_ns = SOURCE_DATETIME_ns;
    }

    public Integer getChangeTimeStamp_ns() {
        return ChangeTimeStamp_ns;
    }

    public void setChangeTimeStamp_ns(Integer ChangeTimeStamp_ns) {
        this.ChangeTimeStamp_ns = ChangeTimeStamp_ns;
    }

    public String getFIX_DATE_st() {
        return FIX_DATE_st;
    }

    public void setFIX_DATE_st(String FIX_DATE_st) {
        this.FIX_DATE_st = FIX_DATE_st;
    }

    public java.time.LocalDateTime getFIX_DATE_dt() {
        return FIX_DATE_dt;
    }

    public void setFIX_DATE_dt(java.time.LocalDateTime FIX_DATE_dt) {
        this.FIX_DATE_dt = FIX_DATE_dt;
    }

    public String getFIX_DATE_ns() {
        return FIX_DATE_ns;
    }

    public void setFIX_DATE_ns(String FIX_DATE_ns) {
        this.FIX_DATE_ns = FIX_DATE_ns;
    }

    public Double getTRDPRC_1() {
        return TRDPRC_1;
    }

    public void setTRDPRC_1(Double TRDPRC_1) {
        this.TRDPRC_1 = TRDPRC_1;
    }

    public Double getACVOL_1() {
        return ACVOL_1;
    }

    public void setACVOL_1(Double ACVOL_1) {
        this.ACVOL_1 = ACVOL_1;
    }

    public Double getVWAP() {
        return VWAP;
    }

    public void setVWAP(Double VWAP) {
        this.VWAP = VWAP;
    }

    public String getPRCTCK_1() {
        return PRCTCK_1;
    }

    public void setPRCTCK_1(String PRCTCK_1) {
        this.PRCTCK_1 = PRCTCK_1;
    }

    public Long getBLKVOLUM() {
        return BLKVOLUM;
    }

    public void setBLKVOLUM(Long BLKVOLUM) {
        this.BLKVOLUM = BLKVOLUM;
    }

    public Long getTOT_VOLUME() {
        return TOT_VOLUME;
    }

    public void setTOT_VOLUME(Long TOT_VOLUME) {
        this.TOT_VOLUME = TOT_VOLUME;
    }

    public Long getVOLUME_ADV() {
        return VOLUME_ADV;
    }

    public void setVOLUME_ADV(Long VOLUME_ADV) {
        this.VOLUME_ADV = VOLUME_ADV;
    }

    public Long getVOLUME_DEC() {
        return VOLUME_DEC;
    }

    public void setVOLUME_DEC(Long VOLUME_DEC) {
        this.VOLUME_DEC = VOLUME_DEC;
    }

    public Long getVOLUME_UNC() {
        return VOLUME_UNC;
    }

    public void setVOLUME_UNC(Long VOLUME_UNC) {
        this.VOLUME_UNC = VOLUME_UNC;
    }

    public Long getISSUES_ADV() {
        return ISSUES_ADV;
    }

    public void setISSUES_ADV(Long ISSUES_ADV) {
        this.ISSUES_ADV = ISSUES_ADV;
    }

    public Long getISSUES_DEC() {
        return ISSUES_DEC;
    }

    public void setISSUES_DEC(Long ISSUES_DEC) {
        this.ISSUES_DEC = ISSUES_DEC;
    }

    public Long getISSUES_UNC() {
        return ISSUES_UNC;
    }

    public void setISSUES_UNC(Long ISSUES_UNC) {
        this.ISSUES_UNC = ISSUES_UNC;
    }

    public Long getNEW_LOWS() {
        return NEW_LOWS;
    }

    public void setNEW_LOWS(Long NEW_LOWS) {
        this.NEW_LOWS = NEW_LOWS;
    }

    public Long getTOT_ISSUES() {
        return TOT_ISSUES;
    }

    public void setTOT_ISSUES(Long TOT_ISSUES) {
        this.TOT_ISSUES = TOT_ISSUES;
    }

    public Long getNEW_HIGHS() {
        return NEW_HIGHS;
    }

    public void setNEW_HIGHS(Long NEW_HIGHS) {
        this.NEW_HIGHS = NEW_HIGHS;
    }

    public Double getNRG_NTBACK() {
        return NRG_NTBACK;
    }

    public void setNRG_NTBACK(Double NRG_NTBACK) {
        this.NRG_NTBACK = NRG_NTBACK;
    }

    public Double getNRG_SWING() {
        return NRG_SWING;
    }

    public void setNRG_SWING(Double NRG_SWING) {
        this.NRG_SWING = NRG_SWING;
    }

    public Double getCMP_YIELD() {
        return CMP_YIELD;
    }

    public void setCMP_YIELD(Double CMP_YIELD) {
        this.CMP_YIELD = CMP_YIELD;
    }

    public Double getORDBK_VWAP() {
        return ORDBK_VWAP;
    }

    public void setORDBK_VWAP(Double ORDBK_VWAP) {
        this.ORDBK_VWAP = ORDBK_VWAP;
    }

    public Double getNAV() {
        return NAV;
    }

    public void setNAV(Double NAV) {
        this.NAV = NAV;
    }

    public Long getEFS_VOL() {
        return EFS_VOL;
    }

    public void setEFS_VOL(Long EFS_VOL) {
        this.EFS_VOL = EFS_VOL;
    }

    public Long getEFP_VOL() {
        return EFP_VOL;
    }

    public void setEFP_VOL(Long EFP_VOL) {
        this.EFP_VOL = EFP_VOL;
    }

    public Double getCOM_BASIS() {
        return COM_BASIS;
    }

    public void setCOM_BASIS(Double COM_BASIS) {
        this.COM_BASIS = COM_BASIS;
    }

    public Double getFPN() {
        return FPN;
    }

    public void setFPN(Double FPN) {
        this.FPN = FPN;
    }

    public Long getMAX_EX_LMT() {
        return MAX_EX_LMT;
    }

    public void setMAX_EX_LMT(Long MAX_EX_LMT) {
        this.MAX_EX_LMT = MAX_EX_LMT;
    }

    public Long getMAX_IM_LMT() {
        return MAX_IM_LMT;
    }

    public void setMAX_IM_LMT(Long MAX_IM_LMT) {
        this.MAX_IM_LMT = MAX_IM_LMT;
    }

    public Long getNRG_VAR() {
        return NRG_VAR;
    }

    public void setNRG_VAR(Long NRG_VAR) {
        this.NRG_VAR = NRG_VAR;
    }

    public Double getQPN() {
        return QPN;
    }

    public void setQPN(Double QPN) {
        this.QPN = QPN;
    }

    public Double getREFM_CRACK() {
        return REFM_CRACK;
    }

    public void setREFM_CRACK(Double REFM_CRACK) {
        this.REFM_CRACK = REFM_CRACK;
    }

    public Double getREFM_TOP() {
        return REFM_TOP;
    }

    public void setREFM_TOP(Double REFM_TOP) {
        this.REFM_TOP = REFM_TOP;
    }

    public Double getSSN_DEMAND() {
        return SSN_DEMAND;
    }

    public void setSSN_DEMAND(Double SSN_DEMAND) {
        this.SSN_DEMAND = SSN_DEMAND;
    }

    public Double getOFFER() {
        return OFFER;
    }

    public void setOFFER(Double OFFER) {
        this.OFFER = OFFER;
    }

    public String getECON_ACT() {
        return ECON_ACT;
    }

    public void setECON_ACT(String ECON_ACT) {
        this.ECON_ACT = ECON_ACT;
    }

    public Double getAVERG_PRC() {
        return AVERG_PRC;
    }

    public void setAVERG_PRC(Double AVERG_PRC) {
        this.AVERG_PRC = AVERG_PRC;
    }

    public Long getOPINT_1() {
        return OPINT_1;
    }

    public void setOPINT_1(Long OPINT_1) {
        this.OPINT_1 = OPINT_1;
    }

    public Long getPRD_NUMMOV() {
        return PRD_NUMMOV;
    }

    public void setPRD_NUMMOV(Long PRD_NUMMOV) {
        this.PRD_NUMMOV = PRD_NUMMOV;
    }

    public Long getTOT_DEMAND() {
        return TOT_DEMAND;
    }

    public void setTOT_DEMAND(Long TOT_DEMAND) {
        this.TOT_DEMAND = TOT_DEMAND;
    }

    public Double getPCTCHNG() {
        return PCTCHNG;
    }

    public void setPCTCHNG(Double PCTCHNG) {
        this.PCTCHNG = PCTCHNG;
    }

    public String getTRADE_ID() {
        return TRADE_ID;
    }

    public void setTRADE_ID(String TRADE_ID) {
        this.TRADE_ID = TRADE_ID;
    }

    public Double getQUTA_REM() {
        return QUTA_REM;
    }

    public void setQUTA_REM(Double QUTA_REM) {
        this.QUTA_REM = QUTA_REM;
    }

    public Double getODD_PRC() {
        return ODD_PRC;
    }

    public void setODD_PRC(Double ODD_PRC) {
        this.ODD_PRC = ODD_PRC;
    }

    public Long getODD_TRDVOL() {
        return ODD_TRDVOL;
    }

    public void setODD_TRDVOL(Long ODD_TRDVOL) {
        this.ODD_TRDVOL = ODD_TRDVOL;
    }

    public Double getODD_TURN() {
        return ODD_TURN;
    }

    public void setODD_TURN(Double ODD_TURN) {
        this.ODD_TURN = ODD_TURN;
    }

    public Double getTOT_BUYVAL() {
        return TOT_BUYVAL;
    }

    public void setTOT_BUYVAL(Double TOT_BUYVAL) {
        this.TOT_BUYVAL = TOT_BUYVAL;
    }

    public Long getTOT_BUYVOL() {
        return TOT_BUYVOL;
    }

    public void setTOT_BUYVOL(Long TOT_BUYVOL) {
        this.TOT_BUYVOL = TOT_BUYVOL;
    }

    public Double getTOT_SELVAL() {
        return TOT_SELVAL;
    }

    public void setTOT_SELVAL(Double TOT_SELVAL) {
        this.TOT_SELVAL = TOT_SELVAL;
    }

    public Long getTOT_SELVOL() {
        return TOT_SELVOL;
    }

    public void setTOT_SELVOL(Long TOT_SELVOL) {
        this.TOT_SELVOL = TOT_SELVOL;
    }

    public String getTRD_P_CCY() {
        return TRD_P_CCY;
    }

    public void setTRD_P_CCY(String TRD_P_CCY) {
        this.TRD_P_CCY = TRD_P_CCY;
    }

    public Double getFLOWS() {
        return FLOWS;
    }

    public void setFLOWS(Double FLOWS) {
        this.FLOWS = FLOWS;
    }

    public Double getTRNOVR_UNS() {
        return TRNOVR_UNS;
    }

    public void setTRNOVR_UNS(Double TRNOVR_UNS) {
        this.TRNOVR_UNS = TRNOVR_UNS;
    }

    public Double getPRIMACT_1() {
        return PRIMACT_1;
    }

    public void setPRIMACT_1(Double PRIMACT_1) {
        this.PRIMACT_1 = PRIMACT_1;
    }

    public Double getTRD_YLD1() {
        return TRD_YLD1;
    }

    public void setTRD_YLD1(Double TRD_YLD1) {
        this.TRD_YLD1 = TRD_YLD1;
    }

    public Double getFAIR_VALUE() {
        return FAIR_VALUE;
    }

    public void setFAIR_VALUE(Double FAIR_VALUE) {
        this.FAIR_VALUE = FAIR_VALUE;
    }

    public Double getFV_CAL_VOL() {
        return FV_CAL_VOL;
    }

    public void setFV_CAL_VOL(Double FV_CAL_VOL) {
        this.FV_CAL_VOL = FV_CAL_VOL;
    }

    public Double getIND_AUC() {
        return IND_AUC;
    }

    public void setIND_AUC(Double IND_AUC) {
        this.IND_AUC = IND_AUC;
    }

    public Double getIND_AUCVOL() {
        return IND_AUCVOL;
    }

    public void setIND_AUCVOL(Double IND_AUCVOL) {
        this.IND_AUCVOL = IND_AUCVOL;
    }

    public Long getSEQ_NO() {
        return SEQ_NO;
    }

    public void setSEQ_NO(Long SEQ_NO) {
        this.SEQ_NO = SEQ_NO;
    }

    public String getTRD_RIC() {
        return TRD_RIC;
    }

    public void setTRD_RIC(String TRD_RIC) {
        this.TRD_RIC = TRD_RIC;
    }

    public String getMMT_CLASS() {
        return MMT_CLASS;
    }

    public void setMMT_CLASS(String MMT_CLASS) {
        this.MMT_CLASS = MMT_CLASS;
    }

    public String getINST_DESC() {
        return INST_DESC;
    }

    public void setINST_DESC(String INST_DESC) {
        this.INST_DESC = INST_DESC;
    }

    public String getTR_TRD_FLG() {
        return TR_TRD_FLG;
    }

    public void setTR_TRD_FLG(String TR_TRD_FLG) {
        this.TR_TRD_FLG = TR_TRD_FLG;
    }

    public String getAGGRS_SID1() {
        return AGGRS_SID1;
    }

    public void setAGGRS_SID1(String AGGRS_SID1) {
        this.AGGRS_SID1 = AGGRS_SID1;
    }

    public Double getTRTN_1W() {
        return TRTN_1W;
    }

    public void setTRTN_1W(Double TRTN_1W) {
        this.TRTN_1W = TRTN_1W;
    }

    public Double getTRTN_1M() {
        return TRTN_1M;
    }

    public void setTRTN_1M(Double TRTN_1M) {
        this.TRTN_1M = TRTN_1M;
    }

    public Double getTRTN_2Y() {
        return TRTN_2Y;
    }

    public void setTRTN_2Y(Double TRTN_2Y) {
        this.TRTN_2Y = TRTN_2Y;
    }

    public Double getTRTN_3Y() {
        return TRTN_3Y;
    }

    public void setTRTN_3Y(Double TRTN_3Y) {
        this.TRTN_3Y = TRTN_3Y;
    }

    public Double getTRTN_4Y() {
        return TRTN_4Y;
    }

    public void setTRTN_4Y(Double TRTN_4Y) {
        this.TRTN_4Y = TRTN_4Y;
    }

    public Double getTRTN_5Y() {
        return TRTN_5Y;
    }

    public void setTRTN_5Y(Double TRTN_5Y) {
        this.TRTN_5Y = TRTN_5Y;
    }

    public Double getMTD_TRTN() {
        return MTD_TRTN;
    }

    public void setMTD_TRTN(Double MTD_TRTN) {
        this.MTD_TRTN = MTD_TRTN;
    }

    public Double getQTD_TRTN() {
        return QTD_TRTN;
    }

    public void setQTD_TRTN(Double QTD_TRTN) {
        this.QTD_TRTN = QTD_TRTN;
    }

    public Double getTRTN() {
        return TRTN;
    }

    public void setTRTN(Double TRTN) {
        this.TRTN = TRTN;
    }

    public Double getYR_TRTN() {
        return YR_TRTN;
    }

    public void setYR_TRTN(Double YR_TRTN) {
        this.YR_TRTN = YR_TRTN;
    }

    public Double getYTD_TRTN() {
        return YTD_TRTN;
    }

    public void setYTD_TRTN(Double YTD_TRTN) {
        this.YTD_TRTN = YTD_TRTN;
    }

    public Double getTRTN_3MT() {
        return TRTN_3MT;
    }

    public void setTRTN_3MT(Double TRTN_3MT) {
        this.TRTN_3MT = TRTN_3MT;
    }

    public Double getO_TRDPRC() {
        return O_TRDPRC;
    }

    public void setO_TRDPRC(Double O_TRDPRC) {
        this.O_TRDPRC = O_TRDPRC;
    }

    public Double getO_TRDVOL() {
        return O_TRDVOL;
    }

    public void setO_TRDVOL(Double O_TRDVOL) {
        this.O_TRDVOL = O_TRDVOL;
    }

    public Double getACVOL_UNS() {
        return ACVOL_UNS;
    }

    public void setACVOL_UNS(Double ACVOL_UNS) {
        this.ACVOL_UNS = ACVOL_UNS;
    }

    public Double getECON_FCAST() {
        return ECON_FCAST;
    }

    public void setECON_FCAST(Double ECON_FCAST) {
        this.ECON_FCAST = ECON_FCAST;
    }

    public Double getECON_PRIOR() {
        return ECON_PRIOR;
    }

    public void setECON_PRIOR(Double ECON_PRIOR) {
        this.ECON_PRIOR = ECON_PRIOR;
    }

    public Double getECON_REV() {
        return ECON_REV;
    }

    public void setECON_REV(Double ECON_REV) {
        this.ECON_REV = ECON_REV;
    }

    public Double getFCAST_NUM() {
        return FCAST_NUM;
    }

    public void setFCAST_NUM(Double FCAST_NUM) {
        this.FCAST_NUM = FCAST_NUM;
    }

    public Double getMKT_CHNG() {
        return MKT_CHNG;
    }

    public void setMKT_CHNG(Double MKT_CHNG) {
        this.MKT_CHNG = MKT_CHNG;
    }

    public Double getMKT_STRN() {
        return MKT_STRN;
    }

    public void setMKT_STRN(Double MKT_STRN) {
        this.MKT_STRN = MKT_STRN;
    }

    public Double getMKT_VOLT() {
        return MKT_VOLT;
    }

    public void setMKT_VOLT(Double MKT_VOLT) {
        this.MKT_VOLT = MKT_VOLT;
    }

    public Double getMKT_WEAK() {
        return MKT_WEAK;
    }

    public void setMKT_WEAK(Double MKT_WEAK) {
        this.MKT_WEAK = MKT_WEAK;
    }

    public Long getMOVES_ADV() {
        return MOVES_ADV;
    }

    public void setMOVES_ADV(Long MOVES_ADV) {
        this.MOVES_ADV = MOVES_ADV;
    }

    public Long getMOVES_DEC() {
        return MOVES_DEC;
    }

    public void setMOVES_DEC(Long MOVES_DEC) {
        this.MOVES_DEC = MOVES_DEC;
    }

    public Long getMOVES_UNC() {
        return MOVES_UNC;
    }

    public void setMOVES_UNC(Long MOVES_UNC) {
        this.MOVES_UNC = MOVES_UNC;
    }

    public Double getPERATIO() {
        return PERATIO;
    }

    public void setPERATIO(Double PERATIO) {
        this.PERATIO = PERATIO;
    }

    public Long getTOT_MOVES() {
        return TOT_MOVES;
    }

    public void setTOT_MOVES(Long TOT_MOVES) {
        this.TOT_MOVES = TOT_MOVES;
    }

    public Double getFVMA_1MM() {
        return FVMA_1MM;
    }

    public void setFVMA_1MM(Double FVMA_1MM) {
        this.FVMA_1MM = FVMA_1MM;
    }

    public Double getFVMA_3MM() {
        return FVMA_3MM;
    }

    public void setFVMA_3MM(Double FVMA_3MM) {
        this.FVMA_3MM = FVMA_3MM;
    }

    public Double getFVMA_5MM() {
        return FVMA_5MM;
    }

    public void setFVMA_5MM(Double FVMA_5MM) {
        this.FVMA_5MM = FVMA_5MM;
    }

    public Double getFVMA_10MM() {
        return FVMA_10MM;
    }

    public void setFVMA_10MM(Double FVMA_10MM) {
        this.FVMA_10MM = FVMA_10MM;
    }

    public Double getFVMA_20MM() {
        return FVMA_20MM;
    }

    public void setFVMA_20MM(Double FVMA_20MM) {
        this.FVMA_20MM = FVMA_20MM;
    }

    public Double getFVMA_30MM() {
        return FVMA_30MM;
    }

    public void setFVMA_30MM(Double FVMA_30MM) {
        this.FVMA_30MM = FVMA_30MM;
    }

    public Double getFVMA_40MM() {
        return FVMA_40MM;
    }

    public void setFVMA_40MM(Double FVMA_40MM) {
        this.FVMA_40MM = FVMA_40MM;
    }

    public Double getFVMA_50MM() {
        return FVMA_50MM;
    }

    public void setFVMA_50MM(Double FVMA_50MM) {
        this.FVMA_50MM = FVMA_50MM;
    }

    public Double getFVMA_60MM() {
        return FVMA_60MM;
    }

    public void setFVMA_60MM(Double FVMA_60MM) {
        this.FVMA_60MM = FVMA_60MM;
    }

    public Double getFVMA_70MM() {
        return FVMA_70MM;
    }

    public void setFVMA_70MM(Double FVMA_70MM) {
        this.FVMA_70MM = FVMA_70MM;
    }

    public Double getFVMA_80MM() {
        return FVMA_80MM;
    }

    public void setFVMA_80MM(Double FVMA_80MM) {
        this.FVMA_80MM = FVMA_80MM;
    }

    public Double getFVMA_90MM() {
        return FVMA_90MM;
    }

    public void setFVMA_90MM(Double FVMA_90MM) {
        this.FVMA_90MM = FVMA_90MM;
    }

    public Double getFVMA_100M() {
        return FVMA_100M;
    }

    public void setFVMA_100M(Double FVMA_100M) {
        this.FVMA_100M = FVMA_100M;
    }

    public Double getFVAC_1MM() {
        return FVAC_1MM;
    }

    public void setFVAC_1MM(Double FVAC_1MM) {
        this.FVAC_1MM = FVAC_1MM;
    }

    public Double getFVAC_3MM() {
        return FVAC_3MM;
    }

    public void setFVAC_3MM(Double FVAC_3MM) {
        this.FVAC_3MM = FVAC_3MM;
    }

    public Double getFVAC_5MM() {
        return FVAC_5MM;
    }

    public void setFVAC_5MM(Double FVAC_5MM) {
        this.FVAC_5MM = FVAC_5MM;
    }

    public Double getFVAC_10MM() {
        return FVAC_10MM;
    }

    public void setFVAC_10MM(Double FVAC_10MM) {
        this.FVAC_10MM = FVAC_10MM;
    }

    public Double getFVAC_20MM() {
        return FVAC_20MM;
    }

    public void setFVAC_20MM(Double FVAC_20MM) {
        this.FVAC_20MM = FVAC_20MM;
    }

    public Double getFVAC_30MM() {
        return FVAC_30MM;
    }

    public void setFVAC_30MM(Double FVAC_30MM) {
        this.FVAC_30MM = FVAC_30MM;
    }

    public Double getFVAC_40MM() {
        return FVAC_40MM;
    }

    public void setFVAC_40MM(Double FVAC_40MM) {
        this.FVAC_40MM = FVAC_40MM;
    }

    public Double getFVAC_50MM() {
        return FVAC_50MM;
    }

    public void setFVAC_50MM(Double FVAC_50MM) {
        this.FVAC_50MM = FVAC_50MM;
    }

    public Double getFVAC_60MM() {
        return FVAC_60MM;
    }

    public void setFVAC_60MM(Double FVAC_60MM) {
        this.FVAC_60MM = FVAC_60MM;
    }

    public Double getFVAC_70MM() {
        return FVAC_70MM;
    }

    public void setFVAC_70MM(Double FVAC_70MM) {
        this.FVAC_70MM = FVAC_70MM;
    }

    public Double getFVAC_80MM() {
        return FVAC_80MM;
    }

    public void setFVAC_80MM(Double FVAC_80MM) {
        this.FVAC_80MM = FVAC_80MM;
    }

    public Double getFVAC_90MM() {
        return FVAC_90MM;
    }

    public void setFVAC_90MM(Double FVAC_90MM) {
        this.FVAC_90MM = FVAC_90MM;
    }

    public Double getFVAC_100M() {
        return FVAC_100M;
    }

    public void setFVAC_100M(Double FVAC_100M) {
        this.FVAC_100M = FVAC_100M;
    }

    public String getRPT_BS_CCY() {
        return RPT_BS_CCY;
    }

    public void setRPT_BS_CCY(String RPT_BS_CCY) {
        this.RPT_BS_CCY = RPT_BS_CCY;
    }

    public String getRPT_P_METH() {
        return RPT_P_METH;
    }

    public void setRPT_P_METH(String RPT_P_METH) {
        this.RPT_P_METH = RPT_P_METH;
    }

    public Double getLTNOV_UNS() {
        return LTNOV_UNS;
    }

    public void setLTNOV_UNS(Double LTNOV_UNS) {
        this.LTNOV_UNS = LTNOV_UNS;
    }

    public Double getTRD_ASP1M() {
        return TRD_ASP1M;
    }

    public void setTRD_ASP1M(Double TRD_ASP1M) {
        this.TRD_ASP1M = TRD_ASP1M;
    }

    public Double getTRD_ASP3M() {
        return TRD_ASP3M;
    }

    public void setTRD_ASP3M(Double TRD_ASP3M) {
        this.TRD_ASP3M = TRD_ASP3M;
    }

    public Double getTRD_ASP6M() {
        return TRD_ASP6M;
    }

    public void setTRD_ASP6M(Double TRD_ASP6M) {
        this.TRD_ASP6M = TRD_ASP6M;
    }

    public Double getTRD_ASP() {
        return TRD_ASP;
    }

    public void setTRD_ASP(Double TRD_ASP) {
        this.TRD_ASP = TRD_ASP;
    }

    public Double getTRD_BPV() {
        return TRD_BPV;
    }

    public void setTRD_BPV(Double TRD_BPV) {
        this.TRD_BPV = TRD_BPV;
    }

    public Double getTRD_BVNINF() {
        return TRD_BVNINF;
    }

    public void setTRD_BVNINF(Double TRD_BVNINF) {
        this.TRD_BVNINF = TRD_BVNINF;
    }

    public Double getTRD_CLN_PR() {
        return TRD_CLN_PR;
    }

    public void setTRD_CLN_PR(Double TRD_CLN_PR) {
        this.TRD_CLN_PR = TRD_CLN_PR;
    }

    public Double getTRD_CMPYLD() {
        return TRD_CMPYLD;
    }

    public void setTRD_CMPYLD(Double TRD_CMPYLD) {
        this.TRD_CMPYLD = TRD_CMPYLD;
    }

    public Double getTRD_CNVXTY() {
        return TRD_CNVXTY;
    }

    public void setTRD_CNVXTY(Double TRD_CNVXTY) {
        this.TRD_CNVXTY = TRD_CNVXTY;
    }

    public Double getTRD_ISMAYL() {
        return TRD_ISMAYL;
    }

    public void setTRD_ISMAYL(Double TRD_ISMAYL) {
        this.TRD_ISMAYL = TRD_ISMAYL;
    }

    public Double getTRD_OAS() {
        return TRD_OAS;
    }

    public void setTRD_OAS(Double TRD_OAS) {
        this.TRD_OAS = TRD_OAS;
    }

    public Double getTRD_SWP_SP() {
        return TRD_SWP_SP;
    }

    public void setTRD_SWP_SP(Double TRD_SWP_SP) {
        this.TRD_SWP_SP = TRD_SWP_SP;
    }

    public Double getTRD_DSCMRG() {
        return TRD_DSCMRG;
    }

    public void setTRD_DSCMRG(Double TRD_DSCMRG) {
        this.TRD_DSCMRG = TRD_DSCMRG;
    }

    public String getMFD_TRANTP() {
        return MFD_TRANTP;
    }

    public void setMFD_TRANTP(String MFD_TRANTP) {
        this.MFD_TRANTP = MFD_TRANTP;
    }

    public String getMFD_NGOTRD() {
        return MFD_NGOTRD;
    }

    public void setMFD_NGOTRD(String MFD_NGOTRD) {
        this.MFD_NGOTRD = MFD_NGOTRD;
    }

    public String getMFD_AGENCY() {
        return MFD_AGENCY;
    }

    public void setMFD_AGENCY(String MFD_AGENCY) {
        this.MFD_AGENCY = MFD_AGENCY;
    }

    public String getMFD_MODTRD() {
        return MFD_MODTRD;
    }

    public void setMFD_MODTRD(String MFD_MODTRD) {
        this.MFD_MODTRD = MFD_MODTRD;
    }

    public String getMFD_REFTRD() {
        return MFD_REFTRD;
    }

    public void setMFD_REFTRD(String MFD_REFTRD) {
        this.MFD_REFTRD = MFD_REFTRD;
    }

    public String getMFD_SP_DIV() {
        return MFD_SP_DIV;
    }

    public void setMFD_SP_DIV(String MFD_SP_DIV) {
        this.MFD_SP_DIV = MFD_SP_DIV;
    }

    public String getMFD_FRMTRD() {
        return MFD_FRMTRD;
    }

    public void setMFD_FRMTRD(String MFD_FRMTRD) {
        this.MFD_FRMTRD = MFD_FRMTRD;
    }

    public String getMFD_ALGTRD() {
        return MFD_ALGTRD;
    }

    public void setMFD_ALGTRD(String MFD_ALGTRD) {
        this.MFD_ALGTRD = MFD_ALGTRD;
    }

    public String getMFD_DEFRSN() {
        return MFD_DEFRSN;
    }

    public void setMFD_DEFRSN(String MFD_DEFRSN) {
        this.MFD_DEFRSN = MFD_DEFRSN;
    }

    public String getMFD_DEFTYP() {
        return MFD_DEFTYP;
    }

    public void setMFD_DEFTYP(String MFD_DEFTYP) {
        this.MFD_DEFTYP = MFD_DEFTYP;
    }

    public String getMFD_DUPTRD() {
        return MFD_DUPTRD;
    }

    public void setMFD_DUPTRD(String MFD_DUPTRD) {
        this.MFD_DUPTRD = MFD_DUPTRD;
    }

    public Double getTRDVOL_ALT() {
        return TRDVOL_ALT;
    }

    public void setTRDVOL_ALT(Double TRDVOL_ALT) {
        this.TRDVOL_ALT = TRDVOL_ALT;
    }

    public Double getTRD_BMKSPD() {
        return TRD_BMKSPD;
    }

    public void setTRD_BMKSPD(Double TRD_BMKSPD) {
        this.TRD_BMKSPD = TRD_BMKSPD;
    }

    public Double getTRD_CDS_BS() {
        return TRD_CDS_BS;
    }

    public void setTRD_CDS_BS(Double TRD_CDS_BS) {
        this.TRD_CDS_BS = TRD_CDS_BS;
    }

    public Double getTRD_SIMMGN() {
        return TRD_SIMMGN;
    }

    public void setTRD_SIMMGN(Double TRD_SIMMGN) {
        this.TRD_SIMMGN = TRD_SIMMGN;
    }

    public Double getTRD_ZSPRD() {
        return TRD_ZSPRD;
    }

    public void setTRD_ZSPRD(Double TRD_ZSPRD) {
        this.TRD_ZSPRD = TRD_ZSPRD;
    }

    public Double getTRD_YTB() {
        return TRD_YTB;
    }

    public void setTRD_YTB(Double TRD_YTB) {
        this.TRD_YTB = TRD_YTB;
    }

    public Double getTRD_YTM() {
        return TRD_YTM;
    }

    public void setTRD_YTM(Double TRD_YTM) {
        this.TRD_YTM = TRD_YTM;
    }

    public Double getTRD_BNDFLR() {
        return TRD_BNDFLR;
    }

    public void setTRD_BNDFLR(Double TRD_BNDFLR) {
        this.TRD_BNDFLR = TRD_BNDFLR;
    }

    public Double getTRD_YTC() {
        return TRD_YTC;
    }

    public void setTRD_YTC(Double TRD_YTC) {
        this.TRD_YTC = TRD_YTC;
    }

    public Double getTRD_YTP() {
        return TRD_YTP;
    }

    public void setTRD_YTP(Double TRD_YTP) {
        this.TRD_YTP = TRD_YTP;
    }

    public Double getTRD_YTW() {
        return TRD_YTW;
    }

    public void setTRD_YTW(Double TRD_YTW) {
        this.TRD_YTW = TRD_YTW;
    }

    public Double getTRD_MODDUR() {
        return TRD_MODDUR;
    }

    public void setTRD_MODDUR(Double TRD_MODDUR) {
        this.TRD_MODDUR = TRD_MODDUR;
    }

    public Double getTRD_PREM() {
        return TRD_PREM;
    }

    public void setTRD_PREM(Double TRD_PREM) {
        this.TRD_PREM = TRD_PREM;
    }

    public Double getAGGR_PRC() {
        return AGGR_PRC;
    }

    public void setAGGR_PRC(Double AGGR_PRC) {
        this.AGGR_PRC = AGGR_PRC;
    }

    public Double getAGGR_VOL() {
        return AGGR_VOL;
    }

    public void setAGGR_VOL(Double AGGR_VOL) {
        this.AGGR_VOL = AGGR_VOL;
    }

    public Integer getAGGR_CNT() {
        return AGGR_CNT;
    }

    public void setAGGR_CNT(Integer AGGR_CNT) {
        this.AGGR_CNT = AGGR_CNT;
    }

    public String getSUM_ACTION_6() {
        return SUM_ACTION_6;
    }

    public void setSUM_ACTION_6(String SUM_ACTION_6) {
        this.SUM_ACTION_6 = SUM_ACTION_6;
    }

    public String getSUM_ACTION_7() {
        return SUM_ACTION_7;
    }

    public void setSUM_ACTION_7(String SUM_ACTION_7) {
        this.SUM_ACTION_7 = SUM_ACTION_7;
    }

    public String getSUM_ACTION_8() {
        return SUM_ACTION_8;
    }

    public void setSUM_ACTION_8(String SUM_ACTION_8) {
        this.SUM_ACTION_8 = SUM_ACTION_8;
    }

    public Double getThirtyD_A_IM_C() {
        return ThirtyD_A_IM_C;
    }

    public void setThirtyD_A_IM_C(Double thirtyD_A_IM_C) {
        ThirtyD_A_IM_C = thirtyD_A_IM_C;
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

    public Double getThirtyD_A_IM_P() {
        return ThirtyD_A_IM_P;
    }

    public void setThirtyD_A_IM_P(Double thirtyD_A_IM_P) {
        ThirtyD_A_IM_P = thirtyD_A_IM_P;
    }

    public Double getSixtyD_A_IM_C() {
        return SixtyD_A_IM_C;
    }

    public void setSixtyD_A_IM_C(Double sixtyD_A_IM_C) {
        SixtyD_A_IM_C = sixtyD_A_IM_C;
    }

    public Double getSixtyD_A_IM_P() {
        return SixtyD_A_IM_P;
    }

    public void setSixtyD_A_IM_P(Double sixtyD_A_IM_P) {
        SixtyD_A_IM_P = sixtyD_A_IM_P;
    }

    public Double getNinetyD_A_IM_C() {
        return NinetyD_A_IM_C;
    }

    public void setNinetyD_A_IM_C(Double ninetyD_A_IM_C) {
        NinetyD_A_IM_C = ninetyD_A_IM_C;
    }

    public Double getNinetyD_A_IM_P() {
        return NinetyD_A_IM_P;
    }

    public void setNinetyD_A_IM_P(Double ninetyD_A_IM_P) {
        NinetyD_A_IM_P = ninetyD_A_IM_P;
    }
}
