package nir.com.neonvegasslots;
import android.content.SharedPreferences;import android.os.Looper;import java.security.MessageDigest;
/** Local atomic grant+receipt, before Play consumption. Never stores raw purchase tokens. */
final class PurchaseDelivery {
 interface Game {String snapshot();void grant(long coins,int energy);void restore(String snapshot);}
 private final SharedPreferences prefs;private final String stateKey;private boolean failed;
 PurchaseDelivery(SharedPreferences p,String key){prefs=p;stateKey=key;}
 boolean blocked(){return failed;}
 static String receipt(String token){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder s=new StringBuilder("purchase_delivered_");for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 boolean deliver(String token,String product,long coins,int energy,Game game){
  if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Purchase delivery must run on UI thread");
  if(failed||token==null||token.isEmpty()||coins<0||energy<0)return false;
  String key=receipt(token);if(prefs.contains(key))return product.equals(prefs.getString(key,null));
  String before=game.snapshot();String after;try{game.grant(coins,energy);after=game.snapshot();}catch(RuntimeException error){failed=true;game.restore(before);return false;}
  if(!prefs.edit().putString(stateKey,after).putString(key,product).commit()){failed=true;game.restore(before);return false;}
  return true;
 }
}
