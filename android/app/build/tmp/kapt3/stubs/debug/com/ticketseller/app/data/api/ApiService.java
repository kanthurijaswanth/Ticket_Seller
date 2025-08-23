package com.ticketseller.app.data.api;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u000e\u0010\u0002\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0004J\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u00a7@\u00a2\u0006\u0002\u0010\u0004\u00a8\u0006\b"}, d2 = {"Lcom/ticketseller/app/data/api/ApiService;", "", "health", "Lcom/ticketseller/app/data/api/Health;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "listings", "", "Lcom/ticketseller/app/data/api/ListingDto;", "app_debug"})
public abstract interface ApiService {
    
    @retrofit2.http.GET(value = "health")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object health(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.ticketseller.app.data.api.Health> $completion);
    
    @retrofit2.http.GET(value = "listings")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object listings(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.ticketseller.app.data.api.ListingDto>> $completion);
}