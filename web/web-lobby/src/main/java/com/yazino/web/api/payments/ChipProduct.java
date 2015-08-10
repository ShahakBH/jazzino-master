package com.yazino.web.api.payments;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Represents a Chip product available for purchase.</p>
 * The following fields will always be present:
 * <ul>
 * <li>productId</li>
 * <li>chips</li>
 * <li>price</li>
 * <li>currencyLabel</li>
 * <li>label</li>
 * </ul>
 * If a promotion is running for the player then <code>promoId</code> will be present and if this product is promoted, then <code>promoChips</code> will be
 * present (remember that a promotion is for all packages but not all packages have a promoted value).
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ChipProduct {
    @JsonProperty
    private String productId;
    @JsonProperty
    private Long promoId;
    @JsonProperty
    private BigDecimal chips;
    @JsonProperty
    private BigDecimal promoChips;
    @JsonProperty
    private String label;
    @JsonProperty
    private BigDecimal price;
    @JsonProperty
    private String currencyLabel;

    private ChipProduct() {

    }

    private ChipProduct(String productId, Long promoId, BigDecimal chips,
                        BigDecimal promoChips, String label, BigDecimal price, String currencyLabel) {
        this.productId = productId;
        this.promoId = promoId;
        this.chips = chips;
        this.promoChips = promoChips;
        this.label = label;
        this.price = price;
        this.currencyLabel = currencyLabel;
    }

    /**
     * The product identifier. The id is store dependent. For some stores, e.g. GooglePlay, this is the external id, i.e. it is the product id on the play
     * store. For others it can be an internal id that is store dependent.
     *
     * @return product identifier.
     */
    public String getProductId() {
        return productId;
    }

    /**
     * The promotion identifier. If this product is not part of a promotion this id will be <code>null<code/>.
     *
     * @return the promotion identifier.
     */
    public Long getPromoId() {
        return promoId;
    }

    /**
     * The price of the product to 2DP
     *
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Currency label e.g. "$"
     *
     * @return currency label
     */
    public String getCurrencyLabel() {
        return currencyLabel;
    }

    /**
     * The default number of chips being purchased.
     *
     * @return default chips.
     */
    public BigDecimal getChips() {
        return chips;
    }

    /**
     * The promoted chip value. If this product is <em>not</em> part of a promotion or the product is <em>not</em>promoted this is <code>null</code>
     *
     * @return
     */
    public BigDecimal getPromoChips() {
        return promoChips;
    }

    /**
     * The product label/description
     *
     * @return label.
     */
    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ChipProduct rhs = (ChipProduct) obj;
        if (this.price != null && this.price.compareTo(rhs.price) != 0) {
            return false;
        }
        if (this.promoChips != null && this.promoChips.compareTo(rhs.promoChips) != 0) {
            return false;
        }
        if (this.chips != null && this.chips.compareTo(rhs.chips) != 0) {
            return false;
        }
        return new EqualsBuilder()
                .append(this.productId, rhs.productId)
                .append(this.promoId, rhs.promoId)
                .append(this.label, rhs.label)
                .append(this.currencyLabel, rhs.currencyLabel)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productId)
                .append(promoId)
                .append(chips)
                .append(promoChips)
                .append(label)
                .append(price)
                .append(currencyLabel)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("productId", productId)
                .append("promoId", promoId)
                .append("chips", chips)
                .append("promoChips", promoChips)
                .append("label", label)
                .append("price", price)
                .append("currencyLabel", currencyLabel)
                .toString();
    }

    public static class ProductBuilder {
        private String productId;
        private Long promoId;
        private BigDecimal chips;
        private BigDecimal promoChips;
        private String label;
        private BigDecimal price;
        private String currencyLabel;

        public ProductBuilder() {
        }

        public ProductBuilder withProductId(String value) {
            this.productId = value;
            return this;
        }

        public ProductBuilder withPromoId(Long value) {
            this.promoId = value;
            return this;
        }

        public ProductBuilder withChips(BigDecimal value) {
            this.chips = value;
            return this;
        }

        public ProductBuilder withPromoChips(BigDecimal value) {
            this.promoChips = value;
            return this;
        }

        public ProductBuilder withLabel(String value) {
            this.label = value;
            return this;
        }

        public ProductBuilder withPrice(BigDecimal value) {
            this.price = value;
            return this;
        }

        public ProductBuilder withCurrencyLabel(String value) {
            this.currencyLabel = value;
            return this;
        }

        public ChipProduct build() {
            return new ChipProduct(productId, promoId, chips, promoChips, label, price, currencyLabel);
        }
    }
}