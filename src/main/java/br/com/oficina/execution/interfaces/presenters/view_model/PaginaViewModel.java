package br.com.oficina.execution.interfaces.presenters.view_model;

import java.util.List;

public record PaginaViewModel<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PaginaViewModel<T> from(List<T> allItems, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int from = Math.min(safePage * safeSize, allItems.size());
        int to = Math.min(from + safeSize, allItems.size());
        int pages = allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / safeSize);
        return new PaginaViewModel<>(List.copyOf(allItems.subList(from, to)), safePage, safeSize, allItems.size(), pages);
    }
}
