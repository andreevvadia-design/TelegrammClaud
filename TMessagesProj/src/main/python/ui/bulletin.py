from java import jclass
from android_utils import run_on_ui_thread
class BulletinHelper:
    @staticmethod
    def show_info(text, *args, **kwargs):
        def show():
            context = jclass("org.telegram.messenger.ApplicationLoader").applicationContext
            jclass("android.widget.Toast").makeText(context,str(text),1).show()
        run_on_ui_thread(show)
    show_error = show_info
    show_success = show_info
