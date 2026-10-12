package br.com.capivara.calculadoracalendario;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
  final int BROWN=Color.rgb(92,49,27), CREAM=Color.rgb(255,246,226); int CARAMEL=0xFFAC6937, GREEN=0xFF5DB542; int theme=0; final String[] THEME_NAMES={"Clássico Aero","Noturno","Minimal","Céu Azul","Jardim","Pôr do Sol","Lavanda","Moranguinho","Café com Leite","Praia","Chuva de Bolhas","Floresta Encantada","Capivara Dourada"}; final int[] TOP={0xFFB8E5E5,0xFF233F60,0xFFE9DED0,0xFF9DDDF8,0xFF9BD5B1,0xFFFFC1A3,0xFFD8C6F6,0xFFFFCED9,0xFFD3B394,0xFF93E4E5,0xFFA5C8E9,0xFF7CA78D,0xFFF3CE79}, BOTTOM={0xFFE6C18D,0xFF796A99,0xFFF9F1E8,0xFFD8F4FF,0xFFECF6C7,0xFFFFE4B7,0xFFF8E9F8,0xFFFFE9D8,0xFFF4E3CC,0xFFFFEDBF,0xFFE5EFF9,0xFFD8E6B7,0xFFFFF0C4}, ACCENT={0xFFAC6937,0xFF69518B,0xFFAA8064,0xFF529CCB,0xFF6B9D63,0xFFDB835A,0xFFA38BC8,0xFFD77F93,0xFF956A4D,0xFF5DB9BF,0xFF7295C6,0xFF628B72,0xFFC59146}, LEAF={0xFF5DB542,0xFF5AA5B8,0xFF8AA782,0xFF67BE95,0xFF4B9B66,0xFF96A967,0xFF91B5A5,0xFF88B77A,0xFF8EAD78,0xFF6AB58B,0xFF81B7B7,0xFF79AF72,0xFF8EAD5B};
  LinearLayout root, body; FrameLayout stage; TextView display, expression; String current="0", operand="", operator=""; boolean fresh=false; Calendar month=Calendar.getInstance(), selected=Calendar.getInstance();
  android.content.SharedPreferences prefs; JSONArray history=new JSONArray(), events=new JSONArray();
  int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);} 
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);configureNavigationWallpaper();prefs=getSharedPreferences("capivara",MODE_PRIVATE);theme=Math.max(0,Math.min(THEME_NAMES.length-1,prefs.getInt("theme",0)));CARAMEL=ACCENT[theme];GREEN=LEAF[theme];try{history=new JSONArray(prefs.getString("history","[]"));events=new JSONArray(prefs.getString("events","[]"));}catch(Exception ignored){}showCalculator();}
  // Estende o wallpaper atrás da barra de navegação sem ocultar os botões do sistema.
  void configureNavigationWallpaper(){
    getWindow().setNavigationBarColor(Color.TRANSPARENT);
    if(Build.VERSION.SDK_INT>=29){
      getWindow().setNavigationBarContrastEnforced(false);
    }
    View decor=getWindow().getDecorView();
    decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }
  // O wallpaper ocupa toda a tela, mas os controles respeitam as barras do Android.
  // Insets são pixels físicos (não converter com dp()).
  void applyNavigationSafeArea(){
    if(Build.VERSION.SDK_INT>=23){
      stage.setOnApplyWindowInsetsListener((v,insets)->{
        if(body!=null){
          int top=insets.getSystemWindowInsetTop();
          int bottom=insets.getSystemWindowInsetBottom();
          int desiredTop=top+dp(12);
          int desiredBottom=bottom+dp(18);
          if(body.getPaddingTop()!=desiredTop || body.getPaddingBottom()!=desiredBottom){
            body.setPadding(dp(16),desiredTop,dp(16),desiredBottom);
          }
        }
        return insets;
      });
      stage.requestApplyInsets();
    }
  }
  int calculatorKeyHeight(){
    // 8% mais baixos que na v1.7, preservando a largura e o efeito de vidro.
    int oldHeight=Math.max(58,Math.min(94,(dpScreenHeight()-385)/5));
    return Math.max(54,Math.round(oldHeight*0.92f));
  }
  GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x80FFFFFF);return d;}
  GradientDrawable gradient(int a,int b,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});d.setCornerRadius(dp(radius));d.setStroke(dp(1),0xAAFFFFFF);return d;}
  TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(BROWN);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,1);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  void pad(View v,int a){v.setPadding(dp(a),dp(a),dp(a),dp(a));}
  void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
  void persist(){prefs.edit().putString("history",history.toString()).putString("events",events.toString()).apply();}
  ImageView image(String asset,int height){ImageView iv=new ImageView(this);try{iv.setImageDrawable(Drawable.createFromStream(getAssets().open(asset),null));}catch(Exception ignored){}iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setBackground(bg(CREAM,25));iv.setClipToOutline(true);iv.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(height)));return iv;}
  // Imagem ocupa todo o fundo. Os controles são desenhados em uma camada acima.
  void base(String title){
    stage=new FrameLayout(this);
    setContentView(stage);
    ImageView wallpaper=new ImageView(this);
    try {
      // Arquivos wallpaper_0.jpg ... wallpaper_12.jpg têm prioridade.
      // Imagens oficiais da versão 1.6. Há fallback para versões anteriores.
      String asset="wallpaper_"+theme+".jpg";
      try { getAssets().open(asset).close(); }
      catch(Exception missing) { asset="theme_"+theme+".jpg"; }
      wallpaper.setImageDrawable(Drawable.createFromStream(getAssets().open(asset),null));
    } catch(Exception ignored) {}
    wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
    stage.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
    // Película translúcida: mantém a arte visível atrás dos controles.
    View tint=new View(this);
    tint.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
      new int[]{0x09000000,0x06FFF5E7,0x16FFF1DA,0x36FFF2D9}));
    stage.addView(tint,new FrameLayout.LayoutParams(-1,-1));
    root=col();root.setBackgroundColor(Color.TRANSPARENT);
    stage.addView(root,new FrameLayout.LayoutParams(-1,-1));
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setClipToPadding(false);
    scroll.setVerticalScrollBarEnabled(false);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=col();body.setPadding(dp(16),dp(12),dp(16),dp(22));scroll.addView(body);applyNavigationSafeArea();
    if(!title.isEmpty()){
      LinearLayout bar=row();bar.setBackground(gradient(0x99FFF8E8,0x66FFFFFF,22));
      TextView back=text("‹",36,true);back.setGravity(Gravity.CENTER);
      bar.addView(back,new LinearLayout.LayoutParams(dp(42),dp(48)));back.setOnClickListener(v->showMenu());
      TextView name=text(title,22,true);name.setGravity(Gravity.CENTER);
      bar.addView(name,new LinearLayout.LayoutParams(0,dp(48),1));
      TextView menu=text("☰",25,true);menu.setGravity(Gravity.CENTER);
      bar.addView(menu,new LinearLayout.LayoutParams(dp(42),dp(48)));menu.setOnClickListener(v->showMenu());
      body.addView(bar);space(body,8);
    }
  }
  GradientDrawable glassButton(int color, boolean operation){
    int top=operation?blend(color,Color.WHITE,0.45f):0xBBFFF9E8;
    int bottom=operation?blend(color,0xFF4B2618,0.13f):0x8AFFE2B6;
    GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
    d.setCornerRadius(dp(24));d.setStroke(dp(1.5f),0xDDFFFFFF);return d;
  }
  int blend(int a,int b,float f){return Color.rgb((int)(Color.red(a)*(1-f)+Color.red(b)*f),(int)(Color.green(a)*(1-f)+Color.green(b)*f),(int)(Color.blue(a)*(1-f)+Color.blue(b)*f));}
  void headerSpacer(int height){space(body,height);}
  void showSplash(){base("");space(body,70);TextView title=text("🌿 Calculadora e\nCalendário\nCapivara 🧡",32,true);title.setGravity(Gravity.CENTER);body.addView(title);space(body,28);headerSpacer(290);space(body,22);TextView sub=text("Seu cantinho fofo para contas e compromissos ✨",16,false);sub.setGravity(Gravity.CENTER);body.addView(sub);space(body,28);button(body,"Começar  ♥",GREEN,()->showCalculator());new Handler().postDelayed(()->{if(root!=null && title.getParent()!=null)showCalculator();},2200);}
  void button(LinearLayout parent,String label,int color,Runnable action){TextView t=text(label,18,true);t.setGravity(Gravity.CENTER);t.setTextColor(color==GREEN?Color.WHITE:BROWN);t.setBackground(glassButton(color,color==GREEN));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.bottomMargin=dp(9);parent.addView(t,lp);t.setOnClickListener(v->action.run());}
  void showCalculator(){base("");LinearLayout top=row();TextView menu=text("☰",28,true);menu.setGravity(Gravity.CENTER);top.addView(menu,new LinearLayout.LayoutParams(dp(50),dp(45)));menu.setOnClickListener(v->showSettings());TextView caption=text("Calculadora 🧡",20,true);caption.setGravity(Gravity.CENTER);top.addView(caption,new LinearLayout.LayoutParams(0,dp(45),1));TextView hist=text("◷",30,true);hist.setGravity(Gravity.CENTER);top.addView(hist,new LinearLayout.LayoutParams(dp(50),dp(45)));hist.setOnClickListener(v->showHistory());top.setBackground(gradient(0x99FFF7E9,0x55FFFFFF,20));body.addView(top);headerSpacer(dpScreenHeight()<720?120:155);space(body,10);
  LinearLayout glass=col();pad(glass,15);glass.setBackground(gradient(0xB5F5FFEA,0x88FFECCF,28));glass.setMinimumHeight(dp(132));body.addView(glass);expression=text(operand.isEmpty()?" ":operand+" "+operator,19,false);expression.setGravity(Gravity.RIGHT);glass.addView(expression);display=text(current,48,true);display.setGravity(Gravity.RIGHT);glass.addView(display);space(body,15);
  String[][] keys={{"C","+/−","%","÷"},{"7","8","9","×"},{"4","5","6","−"},{"1","2","3","+"},{"0",",","⌫","="}};
  for(String[] line:keys){LinearLayout r=row();body.addView(r);for(String key:line){TextView b=text(key,27,true);b.setGravity(Gravity.CENTER);boolean op="÷×−+".contains(key);int c=key.equals("=")?GREEN:op?CARAMEL:CREAM;b.setTextColor(key.equals("=")?Color.WHITE:BROWN);b.setBackground(glassButton(c,op||key.equals("=")));b.setElevation(dp(4));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(calculatorKeyHeight()),1);lp.setMargins(dp(4),dp(4),dp(4),dp(4));r.addView(b,lp);b.setOnClickListener(v->{b.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).withEndAction(()->b.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();press(key);});}}}
  int dpScreenHeight(){return (int)(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density);}
  void press(String k){if(prefs.getBoolean("vib",true))try{((android.os.Vibrator)getSystemService(VIBRATOR_SERVICE)).vibrate(15);}catch(Exception ignored){}if(prefs.getBoolean("sound",false))try{android.media.ToneGenerator tone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC,35);tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,35);new Handler().postDelayed(tone::release,120);}catch(Exception ignored){}try{if(k.matches("[0-9]")){if(fresh||current.equals("0")){current=k;fresh=false;}else if(current.length()<14)current+=k;}else if(k.equals(",")){if(fresh){current="0";fresh=false;}if(!current.contains(","))current+=",";}else if(k.equals("C")){current="0";operand="";operator="";fresh=false;}else if(k.equals("⌫")){current=current.length()>1?current.substring(0,current.length()-1):"0";}else if(k.equals("+/−")){if(!current.equals("0"))current=current.startsWith("-")?current.substring(1):"-"+current;}else if(k.equals("%")){current=format(parse(current)/100);}else if(k.equals("=")){if(!operator.isEmpty()){double a=parse(operand),b=parse(current),result=0;switch(operator){case "+":result=a+b;break;case "−":result=a-b;break;case "×":result=a*b;break;case "÷":if(b==0){current="Erro";operand="";operator="";refresh();return;}result=a/b;}String old=operand+" "+operator+" "+current;current=format(result);JSONObject entry=new JSONObject();entry.put("expr",old);entry.put("result",current);entry.put("time",new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date()));history.put(entry);persist();operand="";operator="";fresh=true;}}else{operand=current;operator=k;fresh=true;}}catch(Exception e){current="Erro";}refresh();}
  double parse(String s){return Double.parseDouble(s.replace(',','.'));}String format(double d){if(Double.isNaN(d)||Double.isInfinite(d))return "Erro";return new DecimalFormat("0.##########",DecimalFormatSymbols.getInstance(new Locale("pt","BR"))).format(d);}void refresh(){if(display!=null)display.setText(current);if(expression!=null)expression.setText(operand.isEmpty()?" ":operand+" "+operator);}
  void showMenu(){showCalculator();}
  void showHistory(){base("Histórico");button(body,"Limpar histórico",CREAM,()->new AlertDialog.Builder(this).setMessage("Apagar todas as contas salvas?").setNegativeButton("Cancelar",null).setPositiveButton("Apagar",(d,w)->{history=new JSONArray();persist();showHistory();}).show());for(int i=history.length()-1;i>=0;i--){JSONObject o=history.optJSONObject(i);if(o==null)continue;LinearLayout card=col();pad(card,15);card.setBackground(bg(0xD9FFFFF0,20));TextView exp=text(o.optString("expr"),19,false),result=text(o.optString("result"),27,true),time=text(o.optString("time"),13,false);card.addView(exp);card.addView(result);card.addView(time);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(9);body.addView(card,lp);card.setOnClickListener(v->{current=o.optString("result");showCalculator();});}if(history.length()==0)body.addView(text("Ainda não há cálculos salvos 🌱",17,false));space(body,20);headerSpacer(140);}
  void showSettings(){base("Configurações");space(body,16);
    LinearLayout introCard=col();pad(introCard,14);introCard.setBackground(gradient(0xEFFFF8EA,0xE5FFE7C9,20));
    TextView title=text("🎨  Escolha sua capivara",21,true);introCard.addView(title);
    TextView intro=text("Cada tema muda a paisagem, as cores e a capivara. Toque para aplicar.",14,false);introCard.addView(intro);
    body.addView(introCard);space(body,12);for(int i=0;i<THEME_NAMES.length;i++){final int selectedTheme=i;LinearLayout themeCard=row();themeCard.setPadding(dp(9),dp(7),dp(10),dp(7));themeCard.setBackground(gradient(0xDCFFF7E9,0xC9FFE3C4,20));ImageView thumb=image("thumb_"+i+".jpg",68);
      thumb.setContentDescription("Miniatura do tema "+THEME_NAMES[i]);
      themeCard.addView(thumb,new LinearLayout.LayoutParams(dp(96),dp(68)));TextView label=text("  "+(theme==i?"✓  ":"")+THEME_NAMES[i],17,true);themeCard.addView(label,new LinearLayout.LayoutParams(0,dp(68),1));LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);margin.bottomMargin=dp(9);body.addView(themeCard,margin);themeCard.setOnClickListener(v->{theme=selectedTheme;CARAMEL=ACCENT[theme];GREEN=LEAF[theme];prefs.edit().putInt("theme",theme).apply();getWindow().setStatusBarColor(TOP[theme]);showSettings();});}Switch sound=new Switch(this);sound.setText("Som dos botões");sound.setTextColor(BROWN);sound.setChecked(prefs.getBoolean("sound",false));body.addView(sound);sound.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean("sound",v).apply());Switch vib=new Switch(this);vib.setText("Vibração");vib.setTextColor(BROWN);vib.setChecked(prefs.getBoolean("vib",true));body.addView(vib);vib.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean("vib",v).apply());space(body,12);button(body,"Limpar histórico de cálculos",CREAM,()->new AlertDialog.Builder(this).setMessage("Limpar histórico?").setNegativeButton("Cancelar",null).setPositiveButton("Limpar",(d,w)->{history=new JSONArray();persist();Toast.makeText(this,"Histórico limpo",Toast.LENGTH_SHORT).show();}).show());}
}
