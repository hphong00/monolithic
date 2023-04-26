package com.web.monolithic.repository.search;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import com.web.monolithic.domain.OrderItemProduct;
import com.web.monolithic.repository.OrderItemProductRepository;
import java.util.List;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.search.sort.SortBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data Elasticsearch repository for the {@link OrderItemProduct} entity.
 */
public interface OrderItemProductSearchRepository
    extends ElasticsearchRepository<OrderItemProduct, UUID>, OrderItemProductSearchRepositoryInternal {}

interface OrderItemProductSearchRepositoryInternal {
    Page<OrderItemProduct> search(String query, Pageable pageable);

    Page<OrderItemProduct> search(Query query);

    void index(OrderItemProduct entity);
}

class OrderItemProductSearchRepositoryInternalImpl implements OrderItemProductSearchRepositoryInternal {

    private final ElasticsearchRestTemplate elasticsearchTemplate;
    private final OrderItemProductRepository repository;

    OrderItemProductSearchRepositoryInternalImpl(ElasticsearchRestTemplate elasticsearchTemplate, OrderItemProductRepository repository) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.repository = repository;
    }

    @Override
    public Page<OrderItemProduct> search(String query, Pageable pageable) {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(queryStringQuery(query));
        return search(nativeSearchQuery.setPageable(pageable));
    }

    @Override
    public Page<OrderItemProduct> search(Query query) {
        SearchHits<OrderItemProduct> searchHits = elasticsearchTemplate.search(query, OrderItemProduct.class);
        List<OrderItemProduct> hits = searchHits.map(SearchHit::getContent).stream().collect(Collectors.toList());
        return new PageImpl<>(hits, query.getPageable(), searchHits.getTotalHits());
    }

    @Override
    public void index(OrderItemProduct entity) {
        repository.findById(entity.getId()).ifPresent(elasticsearchTemplate::save);
    }
}
