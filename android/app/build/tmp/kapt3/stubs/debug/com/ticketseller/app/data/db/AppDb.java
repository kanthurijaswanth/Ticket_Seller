package com.ticketseller.app.data.db;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&\u00a8\u0006\u0005"}, d2 = {"Lcom/ticketseller/app/data/db/AppDb;", "Landroidx/room/RoomDatabase;", "()V", "listingDao", "Lcom/ticketseller/app/data/db/ListingDao;", "app_debug"})
@androidx.room.Database(entities = {com.ticketseller.app.data.db.ListingEntity.class}, version = 1)
public abstract class AppDb extends androidx.room.RoomDatabase {
    
    public AppDb() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.ticketseller.app.data.db.ListingDao listingDao();
}