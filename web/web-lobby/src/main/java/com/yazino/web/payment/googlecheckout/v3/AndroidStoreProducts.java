package com.yazino.web.payment.googlecheckout.v3;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

public class AndroidStoreProducts {
    private Long promoId;

    private Set<AndroidStoreProduct> products;

    public AndroidStoreProducts() {
        this.products = new HashSet<AndroidStoreProduct>();
    }

    public Long getPromoId() {
        return promoId;
    }

    public void setPromoId(Long promoId) {
        this.promoId = promoId;
    }

    public Set<AndroidStoreProduct> getProducts() {
        return products;
    }

    public void setProducts(Set<AndroidStoreProduct> products) {
        this.products = products;
    }

    public void addProduct(AndroidStoreProduct product) {
        products.add(product);
    }

    public AndroidStoreProduct findProductFor(String productId) {
        for (AndroidStoreProduct product : products) {
            if (product.getProductId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final AndroidStoreProducts rhs = (AndroidStoreProducts) obj;
        return new EqualsBuilder()
                .append(promoId, rhs.promoId)
                .append(products, rhs.products)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(promoId)
                .append(products)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(promoId)
                .append(products)
                .toString();
    }
}
