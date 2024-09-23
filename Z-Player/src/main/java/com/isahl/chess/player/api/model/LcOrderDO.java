package com.isahl.chess.player.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * @author xiaojiang.lxj at 2024-09-20 15:07.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LcOrderDO {

    private String order_id;

    private String order_business_type;

    private String company_code;

    private String order_code;

    private String reference_no;

    private String platform;

    private String order_status;

    private String shipping_method;

    private String tracking_no;

    private String carrier_name;

    private String return_slip_number;

    private String warehouse_code;

    private String order_weight;

    private String order_desc;

    private String date_create;

    private String date_release;

    private String date_shipping;

    private String date_modify;

    private String consignee_country_code;

    private String consignee_country_name;

    private String consignee_state;

    private String consignee_city;

    private String consignee_district;

    private String consignee_street1;

    private String consignee_street2;

    private String consignee_street3;

    private String consigne_zipcode;

    private String consignee_doorplate;

    private String consignee_company;

    private String consignee_name;

    private String consignee_phone;

    private String consignee_email;

    private String platform_shop;

    private String currency;

    private String sub_status;

    private String abnormal_reason;

    private String courier_name;

    private Double order_cod_price;

    private String order_cod_currency;

    private String warehouse_note;

    private String create_type;

    private String transfer_order_no;

    private FeeDetails fee_details;

    private String service_number;

    private List<Item> items;

    private List<InventoryBatchOut> inventory_batch_out;

    private String invoice_number;

    private Integer allot_status;

    private String customs_company_name;

    private String customs_address;

    private String customs_contact_name;

    private String customs_email;

    private String customs_tax_code;

    private String customs_phone;

    private String customs_city;

    private String customs_state;

    private String customs_country_code;

    private String customs_postcode;

    private String customs_doorplate;

    private String consignee_tax_number;

    private String order_battery_type;

    private String vat_tax_code;

    private String distribution_information;

    private String consignee_tax_type;

    private String api_source;

    private Integer addressType;

    private String order_volume;

    private String ebay_item_id;

    private String ebay_transaction_id;

    private BatchInfo batch_info;

    private String order_label;

    private Integer is_prime;

    private String order_sale_amount;

    private String order_sale_currency;

    private String is_order_cod;

    private String is_vip;

    private String is_allow_open;

    private String ship_batch_time;

    private List<OrderPackBox> order_pack_box;


    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<InventoryBatchOut> getInventory_batch_out() {
        return inventory_batch_out;
    }

    public void setInventory_batch_out(
        List<InventoryBatchOut> inventory_batch_out) {
        this.inventory_batch_out = inventory_batch_out;
    }

    public BatchInfo getBatch_info() {
        return batch_info;
    }

    public void setBatch_info(BatchInfo batch_info) {
        this.batch_info = batch_info;
    }

    public List<OrderPackBox> getOrder_pack_box() {
        return order_pack_box;
    }

    public void setOrder_pack_box(List<OrderPackBox> order_pack_box) {
        this.order_pack_box = order_pack_box;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getOrder_business_type() {
        return order_business_type;
    }

    public void setOrder_business_type(String order_business_type) {
        this.order_business_type = order_business_type;
    }

    public String getCompany_code() {
        return company_code;
    }

    public void setCompany_code(String company_code) {
        this.company_code = company_code;
    }

    public String getOrder_code() {
        return order_code;
    }

    public void setOrder_code(String order_code) {
        this.order_code = order_code;
    }

    public String getReference_no() {
        return reference_no;
    }

    public void setReference_no(String reference_no) {
        this.reference_no = reference_no;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOrder_status() {
        return order_status;
    }

    public void setOrder_status(String order_status) {
        this.order_status = order_status;
    }

    public String getShipping_method() {
        return shipping_method;
    }

    public void setShipping_method(String shipping_method) {
        this.shipping_method = shipping_method;
    }

    public String getTracking_no() {
        return tracking_no;
    }

    public void setTracking_no(String tracking_no) {
        this.tracking_no = tracking_no;
    }

    public String getCarrier_name() {
        return carrier_name;
    }

    public void setCarrier_name(String carrier_name) {
        this.carrier_name = carrier_name;
    }

    public String getReturn_slip_number() {
        return return_slip_number;
    }

    public void setReturn_slip_number(String return_slip_number) {
        this.return_slip_number = return_slip_number;
    }

    public String getWarehouse_code() {
        return warehouse_code;
    }

    public void setWarehouse_code(String warehouse_code) {
        this.warehouse_code = warehouse_code;
    }

    public String getOrder_weight() {
        return order_weight;
    }

    public void setOrder_weight(String order_weight) {
        this.order_weight = order_weight;
    }

    public String getOrder_desc() {
        return order_desc;
    }

    public void setOrder_desc(String order_desc) {
        this.order_desc = order_desc;
    }

    public String getConsignee_country_code() {
        return consignee_country_code;
    }

    public void setConsignee_country_code(String consignee_country_code) {
        this.consignee_country_code = consignee_country_code;
    }

    public String getConsignee_country_name() {
        return consignee_country_name;
    }

    public void setConsignee_country_name(String consignee_country_name) {
        this.consignee_country_name = consignee_country_name;
    }

    public String getConsignee_state() {
        return consignee_state;
    }

    public void setConsignee_state(String consignee_state) {
        this.consignee_state = consignee_state;
    }

    public String getConsignee_city() {
        return consignee_city;
    }

    public void setConsignee_city(String consignee_city) {
        this.consignee_city = consignee_city;
    }

    public String getConsignee_district() {
        return consignee_district;
    }

    public void setConsignee_district(String consignee_district) {
        this.consignee_district = consignee_district;
    }

    public String getConsignee_street1() {
        return consignee_street1;
    }

    public void setConsignee_street1(String consignee_street1) {
        this.consignee_street1 = consignee_street1;
    }

    public String getConsignee_street2() {
        return consignee_street2;
    }

    public void setConsignee_street2(String consignee_street2) {
        this.consignee_street2 = consignee_street2;
    }

    public String getConsignee_street3() {
        return consignee_street3;
    }

    public void setConsignee_street3(String consignee_street3) {
        this.consignee_street3 = consignee_street3;
    }

    public String getConsigne_zipcode() {
        return consigne_zipcode;
    }

    public void setConsigne_zipcode(String consigne_zipcode) {
        this.consigne_zipcode = consigne_zipcode;
    }

    public String getConsignee_doorplate() {
        return consignee_doorplate;
    }

    public void setConsignee_doorplate(String consignee_doorplate) {
        this.consignee_doorplate = consignee_doorplate;
    }

    public String getConsignee_company() {
        return consignee_company;
    }

    public void setConsignee_company(String consignee_company) {
        this.consignee_company = consignee_company;
    }

    public String getConsignee_name() {
        return consignee_name;
    }

    public void setConsignee_name(String consignee_name) {
        this.consignee_name = consignee_name;
    }

    public String getConsignee_phone() {
        return consignee_phone;
    }

    public void setConsignee_phone(String consignee_phone) {
        this.consignee_phone = consignee_phone;
    }

    public String getConsignee_email() {
        return consignee_email;
    }

    public void setConsignee_email(String consignee_email) {
        this.consignee_email = consignee_email;
    }

    public String getPlatform_shop() {
        return platform_shop;
    }

    public void setPlatform_shop(String platform_shop) {
        this.platform_shop = platform_shop;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSub_status() {
        return sub_status;
    }

    public void setSub_status(String sub_status) {
        this.sub_status = sub_status;
    }

    public String getAbnormal_reason() {
        return abnormal_reason;
    }

    public void setAbnormal_reason(String abnormal_reason) {
        this.abnormal_reason = abnormal_reason;
    }

    public String getCourier_name() {
        return courier_name;
    }

    public void setCourier_name(String courier_name) {
        this.courier_name = courier_name;
    }

    public Double getOrder_cod_price() {
        return order_cod_price;
    }

    public void setOrder_cod_price(Double order_cod_price) {
        this.order_cod_price = order_cod_price;
    }

    public String getOrder_cod_currency() {
        return order_cod_currency;
    }

    public void setOrder_cod_currency(String order_cod_currency) {
        this.order_cod_currency = order_cod_currency;
    }

    public String getWarehouse_note() {
        return warehouse_note;
    }

    public void setWarehouse_note(String warehouse_note) {
        this.warehouse_note = warehouse_note;
    }

    public String getCreate_type() {
        return create_type;
    }

    public void setCreate_type(String create_type) {
        this.create_type = create_type;
    }

    public String getTransfer_order_no() {
        return transfer_order_no;
    }

    public void setTransfer_order_no(String transfer_order_no) {
        this.transfer_order_no = transfer_order_no;
    }

    public FeeDetails getFee_details() {
        return fee_details;
    }

    public void setFee_details(FeeDetails fee_details) {
        this.fee_details = fee_details;
    }

    public String getService_number() {
        return service_number;
    }

    public void setService_number(String service_number) {
        this.service_number = service_number;
    }

    public String getInvoice_number() {
        return invoice_number;
    }

    public void setInvoice_number(String invoice_number) {
        this.invoice_number = invoice_number;
    }

    public Integer getAllot_status() {
        return allot_status;
    }

    public void setAllot_status(Integer allot_status) {
        this.allot_status = allot_status;
    }

    public String getCustoms_company_name() {
        return customs_company_name;
    }

    public void setCustoms_company_name(String customs_company_name) {
        this.customs_company_name = customs_company_name;
    }

    public String getCustoms_address() {
        return customs_address;
    }

    public void setCustoms_address(String customs_address) {
        this.customs_address = customs_address;
    }

    public String getCustoms_contact_name() {
        return customs_contact_name;
    }

    public void setCustoms_contact_name(String customs_contact_name) {
        this.customs_contact_name = customs_contact_name;
    }

    public String getCustoms_email() {
        return customs_email;
    }

    public void setCustoms_email(String customs_email) {
        this.customs_email = customs_email;
    }

    public String getCustoms_tax_code() {
        return customs_tax_code;
    }

    public void setCustoms_tax_code(String customs_tax_code) {
        this.customs_tax_code = customs_tax_code;
    }

    public String getCustoms_phone() {
        return customs_phone;
    }

    public void setCustoms_phone(String customs_phone) {
        this.customs_phone = customs_phone;
    }

    public String getCustoms_city() {
        return customs_city;
    }

    public void setCustoms_city(String customs_city) {
        this.customs_city = customs_city;
    }

    public String getCustoms_state() {
        return customs_state;
    }

    public void setCustoms_state(String customs_state) {
        this.customs_state = customs_state;
    }

    public String getCustoms_country_code() {
        return customs_country_code;
    }

    public void setCustoms_country_code(String customs_country_code) {
        this.customs_country_code = customs_country_code;
    }

    public String getCustoms_postcode() {
        return customs_postcode;
    }

    public void setCustoms_postcode(String customs_postcode) {
        this.customs_postcode = customs_postcode;
    }

    public String getCustoms_doorplate() {
        return customs_doorplate;
    }

    public void setCustoms_doorplate(String customs_doorplate) {
        this.customs_doorplate = customs_doorplate;
    }

    public String getConsignee_tax_number() {
        return consignee_tax_number;
    }

    public void setConsignee_tax_number(String consignee_tax_number) {
        this.consignee_tax_number = consignee_tax_number;
    }

    public String getOrder_battery_type() {
        return order_battery_type;
    }

    public void setOrder_battery_type(String order_battery_type) {
        this.order_battery_type = order_battery_type;
    }

    public String getVat_tax_code() {
        return vat_tax_code;
    }

    public void setVat_tax_code(String vat_tax_code) {
        this.vat_tax_code = vat_tax_code;
    }

    public String getDistribution_information() {
        return distribution_information;
    }

    public void setDistribution_information(String distribution_information) {
        this.distribution_information = distribution_information;
    }

    public String getConsignee_tax_type() {
        return consignee_tax_type;
    }

    public void setConsignee_tax_type(String consignee_tax_type) {
        this.consignee_tax_type = consignee_tax_type;
    }

    public String getApi_source() {
        return api_source;
    }

    public void setApi_source(String api_source) {
        this.api_source = api_source;
    }

    public Integer getAddressType() {
        return addressType;
    }

    public void setAddressType(Integer addressType) {
        this.addressType = addressType;
    }

    public String getOrder_volume() {
        return order_volume;
    }

    public void setOrder_volume(String order_volume) {
        this.order_volume = order_volume;
    }

    public String getEbay_item_id() {
        return ebay_item_id;
    }

    public void setEbay_item_id(String ebay_item_id) {
        this.ebay_item_id = ebay_item_id;
    }

    public String getEbay_transaction_id() {
        return ebay_transaction_id;
    }

    public void setEbay_transaction_id(String ebay_transaction_id) {
        this.ebay_transaction_id = ebay_transaction_id;
    }

    public String getOrder_label() {
        return order_label;
    }

    public void setOrder_label(String order_label) {
        this.order_label = order_label;
    }

    public Integer getIs_prime() {
        return is_prime;
    }

    public void setIs_prime(Integer is_prime) {
        this.is_prime = is_prime;
    }

    public String getOrder_sale_amount() {
        return order_sale_amount;
    }

    public void setOrder_sale_amount(String order_sale_amount) {
        this.order_sale_amount = order_sale_amount;
    }

    public String getOrder_sale_currency() {
        return order_sale_currency;
    }

    public void setOrder_sale_currency(String order_sale_currency) {
        this.order_sale_currency = order_sale_currency;
    }

    public String getIs_order_cod() {
        return is_order_cod;
    }

    public void setIs_order_cod(String is_order_cod) {
        this.is_order_cod = is_order_cod;
    }

    public String getIs_vip() {
        return is_vip;
    }

    public void setIs_vip(String is_vip) {
        this.is_vip = is_vip;
    }

    public String getIs_allow_open() {
        return is_allow_open;
    }

    public void setIs_allow_open(String is_allow_open) {
        this.is_allow_open = is_allow_open;
    }

    public String getDate_create() {
        return date_create;
    }

    public void setDate_create(String date_create) {
        this.date_create = date_create;
    }

    public String getDate_release() {
        return date_release;
    }

    public void setDate_release(String date_release) {
        this.date_release = date_release;
    }

    public String getDate_shipping() {
        return date_shipping;
    }

    public void setDate_shipping(String date_shipping) {
        this.date_shipping = date_shipping;
    }

    public String getDate_modify() {
        return date_modify;
    }

    public void setDate_modify(String date_modify) {
        this.date_modify = date_modify;
    }

    public String getShip_batch_time() {
        return ship_batch_time;
    }

    public void setShip_batch_time(String ship_batch_time) {
        this.ship_batch_time = ship_batch_time;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Item {
        private String product_sku;

        private String quantity;

        private String serial_number;

        private String ebay_item_id;

        private String ebay_transaction_id;

        private Double unit_price;

        public String getProduct_sku() {
            return product_sku;
        }

        public void setProduct_sku(String product_sku) {
            this.product_sku = product_sku;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getSerial_number() {
            return serial_number;
        }

        public void setSerial_number(String serial_number) {
            this.serial_number = serial_number;
        }

        public String getEbay_item_id() {
            return ebay_item_id;
        }

        public void setEbay_item_id(String ebay_item_id) {
            this.ebay_item_id = ebay_item_id;
        }

        public String getEbay_transaction_id() {
            return ebay_transaction_id;
        }

        public void setEbay_transaction_id(String ebay_transaction_id) {
            this.ebay_transaction_id = ebay_transaction_id;
        }

        public Double getUnit_price() {
            return unit_price;
        }

        public void setUnit_price(Double unit_price) {
            this.unit_price = unit_price;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class FeeDetails {

        private String totalFee;

        private String SHIPPING;

        private String OPF;

        private String FSC;

        private String RSF;

        private String WHF;

        private String DT;

        private String OTF;

        private String fee_status;

        private String fee_time;

        public String getTotalFee() {
            return totalFee;
        }

        public void setTotalFee(String totalFee) {
            this.totalFee = totalFee;
        }

        public String getSHIPPING() {
            return SHIPPING;
        }

        public void setSHIPPING(String SHIPPING) {
            this.SHIPPING = SHIPPING;
        }

        public String getOPF() {
            return OPF;
        }

        public void setOPF(String OPF) {
            this.OPF = OPF;
        }

        public String getFSC() {
            return FSC;
        }

        public void setFSC(String FSC) {
            this.FSC = FSC;
        }

        public String getRSF() {
            return RSF;
        }

        public void setRSF(String RSF) {
            this.RSF = RSF;
        }

        public String getWHF() {
            return WHF;
        }

        public void setWHF(String WHF) {
            this.WHF = WHF;
        }

        public String getDT() {
            return DT;
        }

        public void setDT(String DT) {
            this.DT = DT;
        }

        public String getOTF() {
            return OTF;
        }

        public void setOTF(String OTF) {
            this.OTF = OTF;
        }

        public String getFee_status() {
            return fee_status;
        }

        public void setFee_status(String fee_status) {
            this.fee_status = fee_status;
        }

        public String getFee_time() {
            return fee_time;
        }

        public void setFee_time(String fee_time) {
            this.fee_time = fee_time;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class InventoryBatchOut {
        private String receiving_code;

        private String product_barcode;

        private String quantity;

        public String getReceiving_code() {
            return receiving_code;
        }

        public void setReceiving_code(String receiving_code) {
            this.receiving_code = receiving_code;
        }

        public String getProduct_barcode() {
            return product_barcode;
        }

        public void setProduct_barcode(String product_barcode) {
            this.product_barcode = product_barcode;
        }

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class BatchInfo {
        private String product_sku;

        private String sku_quantity;

        private String inventory_code;

        public String getProduct_sku() {
            return product_sku;
        }

        public void setProduct_sku(String product_sku) {
            this.product_sku = product_sku;
        }

        public String getSku_quantity() {
            return sku_quantity;
        }

        public void setSku_quantity(String sku_quantity) {
            this.sku_quantity = sku_quantity;
        }

        public String getInventory_code() {
            return inventory_code;
        }

        public void setInventory_code(String inventory_code) {
            this.inventory_code = inventory_code;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class OrderPackBox {
        private String box_code;

        private String box_num;

        private String box_length;

        private String box_width;

        private String box_height;

        private String box_weight;

        private Integer product_qty;

        private String tracking_number;

        private String pp_barcode;

        private List<ProductDetail> product_details;

        public String getBox_code() {
            return box_code;
        }

        public void setBox_code(String box_code) {
            this.box_code = box_code;
        }

        public String getBox_num() {
            return box_num;
        }

        public void setBox_num(String box_num) {
            this.box_num = box_num;
        }

        public String getBox_length() {
            return box_length;
        }

        public void setBox_length(String box_length) {
            this.box_length = box_length;
        }

        public String getBox_width() {
            return box_width;
        }

        public void setBox_width(String box_width) {
            this.box_width = box_width;
        }

        public String getBox_height() {
            return box_height;
        }

        public void setBox_height(String box_height) {
            this.box_height = box_height;
        }

        public String getBox_weight() {
            return box_weight;
        }

        public void setBox_weight(String box_weight) {
            this.box_weight = box_weight;
        }

        public Integer getProduct_qty() {
            return product_qty;
        }

        public void setProduct_qty(Integer product_qty) {
            this.product_qty = product_qty;
        }

        public String getTracking_number() {
            return tracking_number;
        }

        public void setTracking_number(String tracking_number) {
            this.tracking_number = tracking_number;
        }

        public String getPp_barcode() {
            return pp_barcode;
        }

        public void setPp_barcode(String pp_barcode) {
            this.pp_barcode = pp_barcode;
        }

        public List<ProductDetail> getProduct_details() {
            return product_details;
        }

        public void setProduct_details(
            List<ProductDetail> product_details) {
            this.product_details = product_details;
        }

        public static class ProductDetail {
            private String product_barcode;

            private String sku;

            private String quantity;

            public String getProduct_barcode() {
                return product_barcode;
            }

            public void setProduct_barcode(String product_barcode) {
                this.product_barcode = product_barcode;
            }

            public String getSku() {
                return sku;
            }

            public void setSku(String sku) {
                this.sku = sku;
            }

            public String getQuantity() {
                return quantity;
            }

            public void setQuantity(String quantity) {
                this.quantity = quantity;
            }
        }

    }
}