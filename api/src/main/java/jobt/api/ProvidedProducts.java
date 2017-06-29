package jobt.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink for provided products from upstream task.
 */
public class ProvidedProducts {

    public static final Pattern PATTERN = Pattern.compile("[a-z][a-zA-Z]*");

    private static final Logger LOG = LoggerFactory.getLogger(ProvidedProducts.class);

    private final ProductRepository productRepository;

    private final Set<String> producedProductIds;

    public ProvidedProducts(final Set<String> producedProductIds,
                            final ProductRepository productRepository) {

        Objects.requireNonNull(producedProductIds);
        producedProductIds.forEach(ProvidedProducts::validateProductIdFormat);
        Objects.requireNonNull(productRepository);

        this.producedProductIds = Collections.unmodifiableSet(new HashSet<>(producedProductIds));
        this.productRepository = productRepository;
    }

    public <T extends Product> void complete(final String productId, final T value) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(value,
            "Must not complete product <" + productId + "> with null value");

        if (!producedProductIds.contains(productId)) {
            throw new IllegalStateException(
                "Not allowed to resolve productId <" + productId + ">");
        }

        final ProductPromise productPromise = productRepository.lookup(productId);

        productPromise.complete(value);

        LOG.debug("Product promise <{}> completed with value (type {}): {}",
            productId, value.getClass().getSimpleName(), value);
    }

    public Set<String> getProducedProductIds() {
        return producedProductIds;
    }

    public static void validateProductIdFormat(final String id) {
        if (!ProvidedProducts.PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid format of product id <%s>", id));
        }
    }
}