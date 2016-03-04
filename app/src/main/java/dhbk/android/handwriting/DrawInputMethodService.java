package dhbk.android.handwriting;

import java.util.ArrayList;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

import dhbk.android.handwriting.char_recognizers.CharRecognitionResult;
import dhbk.android.handwriting.char_recognizers.CharRecognizer;
import dhbk.android.handwriting.char_recognizers.RbfSvmCharRecognizer;
import dhbk.android.handwriting.hwr.InputMode;
import dhbk.android.handwriting.widgets.ActionButton;
import dhbk.android.handwriting.widgets.InputModeToggleButton;


public class DrawInputMethodService extends InputMethodService {

	private static final String TAG = "DrawInputMethodService";

	/* How many chars jumps on lon keypress. If set to infinity
	 * crashes on long right click, for some mysterious reason. */
	private static final int LONG_KEYPRESS_LEFT_RIGHT_STEP = 100;

	private View mContainerView;

	private InputModeToggleButton mSmallAbcButton;
	private InputModeToggleButton mBigAbcButton;
	private InputModeToggleButton mNumbersButton;
	private InputModeToggleButton mSpecialCharsButton;
	private Button mEraseButton;
	private ActionButton mActionButton;
	private Button mLeftButton;
	private Button mClearButton;
	private Button mAcceptButton;
	private Button mRightButton;

	private ArrayList<InputModeToggleButton> mInputModeToggleButtons;
	//private ArrayList<InputModeToggleButton> mValidInputModeButtons;
	private ArrayList<InputMode> mValidInputModes;

	private CharRecognizer mCharRecognizer;
	private CharRecognizerController mCharRecognizerController;
	private DrawInputCanvas mCanvas;
	private DrawInputCanvasController mCanvasController;

	private EditorInfo mEditorInfo;

	private InputMode mInputMode;

	@Override
	public void onCreate() {
		Log.d(TAG, TAG + ".onCreate()");
		super.onCreate();
		mCharRecognizerController = new CharRecognizerController();
		mCharRecognizer = new RbfSvmCharRecognizer(this,
				mCharRecognizerController);

	}

	@Override
	public View onCreateInputView() {
		Log.d(TAG, TAG + ".onCreateInputView()");

		mContainerView = getLayoutInflater().inflate(R.layout.drawinput_gui,
				null);
		initReferences(mContainerView);
		updateInputModeButtonStates();
		return mContainerView;

	}

	
	private void updateInputModeButtonStates() {
		mSmallAbcButton.setStateLoaded(mCharRecognizer.isLoaded(InputMode.SMALL_LETTERS));
		mBigAbcButton.setStateLoaded(mCharRecognizer.isLoaded(InputMode.BIG_LETTERS));
		mNumbersButton.setStateLoaded(mCharRecognizer.isLoaded(InputMode.NUMBERS));
		mSpecialCharsButton.setStateLoaded(mCharRecognizer.isLoaded(InputMode.SPECIAL_CHARS));
	}

	private void initReferences(View container) {
		mCanvas = (DrawInputCanvas) container.findViewById(R.id.canvas);
		mCanvasController = new DrawInputCanvasController();
		mCanvas.addCanvasListener(mCanvasController);

		mSmallAbcButton = (InputModeToggleButton) container
				.findViewById(R.id.button_small_abc);
		mBigAbcButton = (InputModeToggleButton) container
				.findViewById(R.id.button_big_abc);
		mNumbersButton = (InputModeToggleButton) container
				.findViewById(R.id.button_numbers);
		mSpecialCharsButton = (InputModeToggleButton) container
				.findViewById(R.id.button_special_chars);

		mSmallAbcButton.setInputMode(InputMode.SMALL_LETTERS);
		mBigAbcButton.setInputMode(InputMode.BIG_LETTERS);
		mNumbersButton.setInputMode(InputMode.NUMBERS);
		mSpecialCharsButton.setInputMode(InputMode.SPECIAL_CHARS);

		mEraseButton = (Button) container.findViewById(R.id.button_erase);
		mActionButton = (ActionButton) container.findViewById(R.id.button_action);

		mLeftButton = (Button) container.findViewById(R.id.button_left);
		mClearButton = (Button) container.findViewById(R.id.button_clear);
		mAcceptButton = (Button) container.findViewById(R.id.button_accept);
		mRightButton = (Button) container.findViewById(R.id.button_right);

		mInputModeToggleButtons = new ArrayList<InputModeToggleButton>(4);
		mInputModeToggleButtons.add(mSmallAbcButton);
		mInputModeToggleButtons.add(mBigAbcButton);
		mInputModeToggleButtons.add(mNumbersButton);
		mInputModeToggleButtons.add(mSpecialCharsButton);

		/* There is no XML attribute for long key press. Have to attach
		 * it in code. */
		mEraseButton.setOnLongClickListener( new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onEraseLongClick();
			}
		});
		mLeftButton.setOnLongClickListener( new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onLeftLongClick();
			}			
		});
		mRightButton.setOnLongClickListener( new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				return onRightLongClick();
			}			
		});
	}

	private boolean onEraseLongClick() {
		InputConnection ic = getCurrentInputConnection();
		CharSequence charsBeforeCursor = ic.getTextBeforeCursor(Integer.MAX_VALUE, 0);
		ic.deleteSurroundingText(charsBeforeCursor.length(), 0);
		mEraseButton.setPressed(false); // weird that this has to be here
		cursorMove();
		return true;
	}

	private boolean onLeftLongClick() {
		InputConnection ic = getCurrentInputConnection();
		int charsBeforeCursor = ic.getTextBeforeCursor(Integer.MAX_VALUE, 0).length();
		ic.commitText("", -1*charsBeforeCursor);
		cursorMove();
		mLeftButton.setPressed(false);
		return true;
	}
	private boolean onRightLongClick() {
		InputConnection ic = getCurrentInputConnection();
		CharSequence str_after = ic.getTextAfterCursor(LONG_KEYPRESS_LEFT_RIGHT_STEP, 0);
		int charsAfterCursor = str_after.length();
		ic.commitText("", charsAfterCursor+1);
		mRightButton.setPressed(false);
		cursorMove();
		return true;
	}

	private void initInputMode() {
		mValidInputModes = new ArrayList<InputMode>();
		
		switch (mEditorInfo.inputType & InputType.TYPE_MASK_CLASS) {
		case InputType.TYPE_CLASS_TEXT:
			mValidInputModes.add(InputMode.SMALL_LETTERS);
			mValidInputModes.add(InputMode.BIG_LETTERS);
			mValidInputModes.add(InputMode.NUMBERS);
			mValidInputModes.add(InputMode.SPECIAL_CHARS);
			setInputMode(InputMode.SMALL_LETTERS);
			break;
		case InputType.TYPE_CLASS_DATETIME:
		case InputType.TYPE_CLASS_NUMBER:
		case InputType.TYPE_CLASS_PHONE:
			mValidInputModes.add(InputMode.NUMBERS);
			setInputMode(InputMode.NUMBERS);
		default:
			break;
		}
	}


	private void setInputMode(InputMode inputmode) {
		updateButtonGroup(inputmode);
		mInputMode = inputmode;
		mCharRecognizer.setInputMode(inputmode);
		mCanvas.setInputMode(inputmode);
	}

	private void updateButtonGroup(InputMode inputmode) {
		for (int i = 0; i < mInputModeToggleButtons.size(); i++) {
			InputModeToggleButton b = mInputModeToggleButtons.get(i);
			if(b.getInputMode() == inputmode){
				b.setChecked(true);
				b.setEnabled(false);
			}
			else{
				b.setChecked(false);
				if(mValidInputModes.contains(b.getInputMode())){
					b.setEnabled(true);
				}
				else{
					b.setEnabled(false);
				}
			}
			
		}
	}

	private InputModeToggleButton getInputModeButton() {
		for (InputModeToggleButton b : mInputModeToggleButtons) {
			if (b.getInputMode() == mInputMode)
				return b;
		}
		return null;
	}

	private void appendText(String str) {
		InputConnection ic = getCurrentInputConnection();

		ic.commitText(str, 1);

		cursorMove();
	}

	private void removeChar() {
		InputConnection ic = getCurrentInputConnection();
		ic.deleteSurroundingText(1, 0);
		cursorMove();
	}

	private void cursorMove() {

		InputConnection ic = getCurrentInputConnection();
		
		if(ic != null){
			CharSequence text = ic.getTextBeforeCursor(10, 0);
			int charsBeforeCursor = (text != null) ? text.length() : 0;
			text = ic.getTextAfterCursor(10, 0);
			int charsAfterCursor = (text != null) ? text.length() : 0;
			
			mEraseButton.setEnabled(charsBeforeCursor > 0);
			mLeftButton.setEnabled(charsBeforeCursor > 0);
			mRightButton.setEnabled(charsAfterCursor > 0);	
		}
		else{
			Log.d(TAG, "WARNING: input connection null @ cursorMove()");
		}
		
	}

    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
    	Log.d(TAG, TAG + ".onInitializeInterface()" );
    	
    }
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		Log.d(TAG, TAG + ".onStartInputView(), restarting = " + restarting);
		super.onStartInputView(info, restarting);
		mEditorInfo = info;
		initInputMode();
		initActionButton();
		cursorMove();
	}

	private void initActionButton() {
		Log.d(TAG, "initGoButton(), action = " + (mEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION));
		
		switch (mEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION) {

			case EditorInfo.IME_ACTION_GO:
				mActionButton.setEnabled(true);
				mActionButton.setActionId(ActionButton.GO);
				break;
				
			case EditorInfo.IME_ACTION_SEARCH:
				mActionButton.setEnabled(true);
				mActionButton.setActionId(ActionButton.SEARCH);
				break;
			case EditorInfo.IME_ACTION_NEXT:
				mActionButton.setEnabled(true);
				mActionButton.setActionId(ActionButton.NEXT);
				break;
			case EditorInfo.IME_ACTION_SEND:
				mActionButton.setEnabled(true);
				mActionButton.setActionId(ActionButton.SEND);
				break;
			case EditorInfo.IME_ACTION_DONE:
				mActionButton.setEnabled(true);
				mActionButton.setActionId(ActionButton.DONE);
				break;
			default:
				mActionButton.setEnabled(false);
				mActionButton.setActionId(ActionButton.NONE);
				break;
		}
	}

	public void onSmallAbcClicked(View v) {
		Log.d(TAG, "onSmallAbcClicked()");
		setInputMode(InputMode.SMALL_LETTERS);
	}

	public void onBigAbcClicked(View v) {
		Log.d(TAG, "onBigAbcClicked()");
		setInputMode(InputMode.BIG_LETTERS);
	}

	public void onNumbersClicked(View v) {
		Log.d(TAG, "onNumbersClicked()");
		setInputMode(InputMode.NUMBERS);
	}

	public void onSpecialCharsClicked(View v) {
		Log.d(TAG, "onSpecialCharsClicked()");
		setInputMode(InputMode.SPECIAL_CHARS);
	}

	public void onEraseClicked(View v) {
		Log.d(TAG, "onEraseClicked()");

		removeChar();
		
	}

	public void onSpaceClicked(View v) {
		Log.d(TAG, "onSpaceClicked()");

		appendText(" ");
		
	}

	public void onEnterClicked(View v) {
		Log.d(TAG, "onEnterClicked()");
		appendText("\n");
	}

	public void onActionClicked(View v) {
		Log.d(TAG, "onActionClicked()");
		getCurrentInputConnection().performEditorAction(mEditorInfo.imeOptions & EditorInfo.IME_MASK_ACTION);
	}

	public void onLeftClicked(View v) {
		Log.d(TAG, "onLeftClicked()");
		InputConnection ic = getCurrentInputConnection();
		ic.commitText("", -1);
		cursorMove();
	}

	public void onClearClicked(View v) {
		Log.d(TAG, "onClearClicked()");

		mCanvas.clear();
		mClearButton.setEnabled(false);
		mAcceptButton.setText(R.string.button_accept_nochar);
		mAcceptButton.setEnabled(false);
	}

	public void onAcceptClicked(View v) {
		Log.d(TAG, "onAcceptClicked()");

		appendText(mAcceptButton.getText().toString());
		mCanvas.clear();
		mClearButton.setEnabled(false);
		mAcceptButton.setText(R.string.button_accept_nochar);
		mAcceptButton.setEnabled(false);
	}

	public void onRightClicked(View v) {
		Log.d(TAG, "onRightClicked()");
		InputConnection ic = getCurrentInputConnection();
		ic.commitText("", 2);
		cursorMove();

	}


	private void disableValidInputModeButtons() {
		for (InputModeToggleButton b : mInputModeToggleButtons) {
			b.setEnabled(false);
		}
	}


	private void enableValidInputModeButtons() {
		for (InputModeToggleButton b : mInputModeToggleButtons) {
			if( b.getInputMode() != mInputMode && mValidInputModes.contains(b.getInputMode()) ){
				b.setEnabled(true);
			}
				
		}
	}


	public class DrawInputCanvasController implements DrawInputCanvas.DrawInputCanvasListener {

		@Override
		public void onNewStroke(DrawInputCanvas canvas) {
			mClearButton.setEnabled(true);
			mCharRecognizer.tryRecognition(canvas.getCharBeingDrawn());
		}

		@Override
		public void onSwipeLeft(DrawInputCanvas canvas) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSwipeRight(DrawInputCanvas canvas) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onCanvasSizeChanged(int w, int h) {
			mCharRecognizer.setCanvasWidth(w);
			mCharRecognizer.setCanvasHeight(h);
		}

	}

	public class CharRecognizerController implements CharRecognizer.CharRecognizerListener {


		@Override
		public void onNoResult() {
			Log.d(TAG, "onNoResult()");
		}

		@Override
		public void onRecognizedChar(CharRecognitionResult result) {
			Log.d(TAG,
					"onRecognizedChar(), result.getChar() = "
							+ result.getChar());
			mAcceptButton.setText("" + result.getChar());
			mAcceptButton.setEnabled(true);
		}

		@Override
		public void onNewInputModeLoaded(InputMode mode) {
			Log.d(TAG, "onNewInputModeLoaded()");
			InputModeToggleButton b = getInputModeButton();
			b.setStateLoaded(true);
			mCanvas.stopInputModeLoading();
			enableValidInputModeButtons();
			
		}

		@Override
		public void onNewInputModeLoading(InputMode mode) {
			Log.d(TAG, "onNewInputModeLoading()");
			mCanvas.showInputModeLoading(mode);
			disableValidInputModeButtons();
		}
		
		

	}

}
