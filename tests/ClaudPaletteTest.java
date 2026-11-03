import org.telegram.messenger.ClaudPalette;
public class ClaudPaletteTest {
 public static void main(String[] args) {
  int[] keep={0xFFFFFFFF,0xFF000000,0xFFE06A9F,0xFF33AA55,0xFFDD3344,45,0};
  for(int c:keep) if(ClaudPalette.warm(c)!=c) throw new AssertionError("Non-blue color changed");
  int[] blues={0xFF2196F3,0xFF5288C1,0xFF263F55,0x662196F3};
  for(int c:blues) {
   int r=ClaudPalette.warm(c);
   if(r==c || (r>>>24)!=(c>>>24)) throw new AssertionError("Blue or alpha not handled");
   if(ClaudPalette.warm(r)!=r) throw new AssertionError("Not idempotent");
  }
  if(!ClaudPalette.isLove("❤️") || !ClaudPalette.isLove("💕") || ClaudPalette.isLove("🌙") || ClaudPalette.isLove(null)) throw new AssertionError("Theme identity");
  for(String emoji:new String[]{"💙","💜","🖤","🤍","🩷","🩵"}) if(!ClaudPalette.isLove(emoji)) throw new AssertionError("Missing heart "+emoji);
  System.out.println("Palette checks passed");
 }
}
