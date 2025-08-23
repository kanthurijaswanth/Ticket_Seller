package com.ticketseller.app.data.repo;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH\u0086@\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u000b\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/ticketseller/app/data/repo/ListingRepository;", "", "api", "Lcom/ticketseller/app/data/api/ApiService;", "db", "Lcom/ticketseller/app/data/db/AppDb;", "(Lcom/ticketseller/app/data/api/ApiService;Lcom/ticketseller/app/data/db/AppDb;)V", "local", "", "Lcom/ticketseller/app/data/db/ListingEntity;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh", "", "app_debug"})
public final class ListingRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.ticketseller.app.data.api.ApiService api = null;
    @org.jetbrains.annotations.NotNull()
    private final com.ticketseller.app.data.db.AppDb db = null;
    
    @javax.inject.Inject()
    public ListingRepository(@org.jetbrains.annotations.NotNull()
    com.ticketseller.app.data.api.ApiService api, @org.jetbrains.annotations.NotNull()
    com.ticketseller.app.data.db.AppDb db) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object refresh(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object local(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ticketseller.app.data.db.ListingEntity>> $completion) {
        return null;
    }
}