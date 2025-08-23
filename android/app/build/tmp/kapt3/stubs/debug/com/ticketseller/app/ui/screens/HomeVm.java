package com.ticketseller.app.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R7\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\u00062\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u00068F@BX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\r\u0010\u000e\u001a\u0004\b\t\u0010\n\"\u0004\b\u000b\u0010\f\u00a8\u0006\u000f"}, d2 = {"Lcom/ticketseller/app/ui/screens/HomeVm;", "Landroidx/lifecycle/ViewModel;", "api", "Lcom/ticketseller/app/data/api/ApiService;", "(Lcom/ticketseller/app/data/api/ApiService;)V", "<set-?>", "", "Lcom/ticketseller/app/data/api/ListingDto;", "listings", "getListings", "()Ljava/util/List;", "setListings", "(Ljava/util/List;)V", "listings$delegate", "Landroidx/compose/runtime/MutableState;", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HomeVm extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ticketseller.app.data.api.ApiService api = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState listings$delegate = null;
    
    @javax.inject.Inject()
    public HomeVm(@org.jetbrains.annotations.NotNull()
    com.ticketseller.app.data.api.ApiService api) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.ticketseller.app.data.api.ListingDto> getListings() {
        return null;
    }
    
    private final void setListings(java.util.List<com.ticketseller.app.data.api.ListingDto> p0) {
    }
}