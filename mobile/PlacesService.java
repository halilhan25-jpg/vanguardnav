package tr.vanguardnav.field;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.car.app.*;
import androidx.car.app.model.*;
import androidx.car.app.validation.HostValidator;
import org.json.*;
public class PlacesService extends CarAppService {
 @NonNull @Override public HostValidator createHostValidator(){return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;}
 @NonNull @Override public Session onCreateSession(){
  return new Session(){@NonNull @Override public Screen onCreateScreen(@NonNull Intent intent){
   return new Screen(getCarContext()){@NonNull @Override public Template onGetTemplate(){
    ItemList.Builder list=new ItemList.Builder();
    list.setNoItemsMessage("Telefonda seyyah defterine bir nokta ekle.");
    try{
     JSONArray points=new JSONArray(getCarContext().getSharedPreferences("car",0).getString("places","[]"));
     for(int i=0;i<Math.min(6,points.length());i++){
      JSONObject p=points.getJSONObject(i);String name=p.getString("name");double lat=p.getDouble("lat"),lon=p.getDouble("lng");
      Place site=new Place.Builder(CarLocation.create(lat,lon)).build();
      list.addItem(new Row.Builder().setTitle(name).setMetadata(new Metadata.Builder().setPlace(site).build()).setOnClickListener(()->{
       Intent nav=new Intent(CarContext.ACTION_NAVIGATE,Uri.parse("geo:"+lat+","+lon+"?q="+lat+","+lon));getCarContext().startCarApp(nav);
      }).build());
     }
    }catch(JSONException ignored){}
    return new PlaceListMapTemplate.Builder().setTitle("VanguardNav · Saha noktaları").setItemList(list.build()).setHeaderAction(Action.APP_ICON).build();
   }};
  }};
 }
}
