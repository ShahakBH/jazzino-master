package com.yazino.web.api.payments;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A collection of all available chip products. The product ids of teh 'best' amd most popular products are tagged.
 *
 * @see ChipProduct
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ChipProducts {
    @JsonProperty
    private String bestProductId;
    @JsonProperty
    private String mostPopularProductId;
    @JsonProperty
    private List<ChipProduct> chipProducts;

    public ChipProducts() {
    }

    private ChipProducts(String bestProductId, String mostPopularProductId, List<ChipProduct> chipProducts) {
        this.bestProductId = bestProductId;
        this.mostPopularProductId = mostPopularProductId;
        this.chipProducts = chipProducts;
    }

    /**
     * Product identifier of the best value product. Can be <code>null</code>.
     *
     * @return best value product identifier.
     */
    public String getBestProductId() {
        return bestProductId;
    }

    /**
     * Product identifier of the most popular product. Can be <code>null</code>.
     *
     * @return
     */
    public String getMostPopularProductId() {
        return mostPopularProductId;
    }

    /**
     * All currently available products.
     *
     * @return available products, or empty list if none.
     * @see ChipProduct
     */
    public List<ChipProduct> getChipProducts() {
        if (chipProducts == null) {
            return Collections.emptyList();
        }
        return chipProducts;
    }

    public void addChipProduct(ChipProduct chipProduct) {
        if (chipProducts == null) {
            chipProducts = new ArrayList<>();
        }
        chipProducts.add(chipProduct);
    }

    public void setBestProductId(String bestProductId) {
        this.bestProductId = bestProductId;
    }

    public void setMostPopularProductId(String mostPopularProductId) {
        this.mostPopularProductId = mostPopularProductId;
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
        ChipProducts rhs = (ChipProducts) obj;
        return new EqualsBuilder()
                .append(this.bestProductId, rhs.bestProductId)
                .append(this.mostPopularProductId, rhs.mostPopularProductId)
                .append(this.chipProducts, rhs.chipProducts)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(bestProductId)
                .append(mostPopularProductId)
                .append(chipProducts)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("bestProductId", bestProductId)
                .append("mostPopularProductId", mostPopularProductId)
                .append("products", chipProducts)
                .toString();
    }
}