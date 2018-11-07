package com.tgx.chess.spring.biz.bill.pay.dao;

import javax.persistence.*;

import com.tgx.chess.spring.jpa.model.AuditModel;

@Entity
@Table(schema = "\"tgx-z-chess-bill-pay\"",
       indexes = { @Index(name = "bill_idx_order", columnList = "order"),
                   @Index(name = "bill_idx_mac", columnList = "mac"),
                   @Index(name = "bill_idx_open_id", columnList = "openId") })
public class BillEntity
        extends
        AuditModel
{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long   id;

    @Column(length = 32)
    private String order;

    @Column(length = 28)
    private String openId;

    @Column(length = 17)
    private String mac;

    @Column
    private double amount;

    @Column(length = 16)
    private String type;

    @Column(length = 20)
    private String result;

}
