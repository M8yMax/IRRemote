package org.twinone.irremote.components;

import java.util.ArrayList;

import org.twinone.irremote.R;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class RemoteOrganizer {

	private final Context mContext;

	public void addFlag(int flag) {
		mFlags |= flag;
	}

	public void clearFlag(int flag) {
		mFlags &= ~flag;
	}

	public void setFlags(int flags) {
		mFlags = flags;
	}

	public static final int FLAG_TEXT = 1 << 0;
	public static final int FLAG_ICON = 1 << 1;
	public static final int FLAG_COLOR = 1 << 2;
	public static final int FLAG_POSITION = 1 << 3;
	public static final int FLAG_TEXT_SIZE = 1 << 4;
	public static final int FLAG_CORNERS = 1 << 5;
	private static final int DEFAULT_FLAGS = FLAG_ICON | FLAG_POSITION
			| FLAG_COLOR | FLAG_CORNERS;

	private int mFlags = DEFAULT_FLAGS;

	private static final int DEFAULT_CORNER_RADIUS = 16; // dp

	// all in px
	private int mMarginLeft; // px
	private int mMarginTop; // px
	private int mGridSpacingX;
	private int mGridSpacingY;
	// We can use width because we'll fill the whole screen's available width
	private int mDeviceWidth;
	private int mAvailableBlocksX;

	private int mGridSizeX;
	private int mGridSizeY;

	private int mBlocksPerButtonX;
	private int mBlocksPerButtonY;

	private int mCols;

	private Remote mRemote;
	/** List of buttons that are already organized */
	private ArrayList<Button> mOrganizedButtons = new ArrayList<Button>();

	/**
	 * Removes a button from the remote and adds it to the organized buttons
	 * list
	 */
	private void moveToOrganizedList(Button... buttons) {
		for (Button b : buttons) {
			// b can be null but we don't want it in the remote
			if (b != null) {
				mOrganizedButtons.add(b);
				mRemote.removeButton(b);
			}
		}
	}

	public RemoteOrganizer(Context c) {
		mContext = c;

		WindowManager wm = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		// Point p = new Point();
		// wm.getDefaultDisplay().getSize(p);
		// mDeviceWidth = p.x;
		mDeviceWidth = metrics.widthPixels;

		mMarginLeft = c.getResources().getDimensionPixelSize(
				R.dimen.grid_min_margin_x);
		mMarginTop = c.getResources().getDimensionPixelSize(
				R.dimen.grid_min_margin_y);

		mGridSizeX = c.getResources()
				.getDimensionPixelSize(R.dimen.grid_size_x);
		mGridSizeY = c.getResources()
				.getDimensionPixelSize(R.dimen.grid_size_y);
		mGridSpacingX = c.getResources().getDimensionPixelSize(
				R.dimen.grid_spacing_x);
		mGridSpacingY = c.getResources().getDimensionPixelSize(
				R.dimen.grid_spacing_y);

		mBlocksPerButtonX = c.getResources().getInteger(
				R.integer.blocks_per_button_x);
		mBlocksPerButtonY = c.getResources().getInteger(
				R.integer.blocks_per_button_y);

		int mAvailableScreenWidth = mDeviceWidth - mMarginLeft * 2
				+ mGridSpacingX;
		Log.d("RemoteOrganizer", "mAvailableScreenWidth: "
				+ pxToDp(mAvailableScreenWidth));

		mAvailableBlocksX = mAvailableScreenWidth / mGridSizeX;
		Log.d("RemoteOrganizer", "Av blockx: " + mAvailableBlocksX);

	}

	/**
	 * Set the margins according to how much block we're going to use
	 */
	private void useCols(int cols) {
		mMarginLeft = (mDeviceWidth - (mGridSizeX * cols * mBlocksPerButtonX - mGridSpacingX)) / 2;
		mAvailableBlocksX = cols * mBlocksPerButtonX;
		mCols = cols;
	}

	/**
	 * Set the offset of the button plus an additional offset in button's size
	 * 
	 * @param b
	 *            The button
	 * @param x
	 *            Offset in blocks from left
	 * @param y
	 *            Offset in blocks from top
	 * @param buttonX
	 *            Offset in button sizes from left
	 * @param buttonY
	 *            Offset in button sizes from right
	 */
	private void setButtonPosition(Button b, int x, int y, int buttonX,
			int buttonY) {
		if (b != null) {
			b.x = mMarginLeft + x * mGridSizeX
					+ (buttonX * mGridSizeX * mBlocksPerButtonX);
			b.y = mMarginTop + y * mGridSizeY
					+ (buttonY * mGridSizeY * mBlocksPerButtonY);
		}
	}

	private void setButtonSize(Button b, int w, int h) {
		if (b != null) {
			b.w = w * mGridSizeX - mGridSpacingX;
			b.h = h * mGridSizeY - mGridSpacingY;
		}
	}

	private void setButtonCornerDp(Button b, int dp) {
		if (b != null) {
			b.setCornerRadius(dpToPx(dp));
		}
	}

	private float dpToPx(float dp) {
		return dp * mContext.getResources().getDisplayMetrics().density;
	}

	private float pxToDp(float px) {
		return px / mContext.getResources().getDisplayMetrics().density;
	}

	/** Add default icons to this remote's buttons based on their ID's */
	public static void addIcons(Remote remote, boolean removeTextIfIconFound) {
		for (Button b : remote.buttons) {
			int icon = ComponentUtils.getIconIdForCommonButton(b.id);
			b.ic = icon;
			if (icon != 0 && removeTextIfIconFound)
				b.text = null;
		}
	}

	public void updateAndSaveAll() {
		for (String name : Remote.getNames(mContext)) {
			updateAndSave(name);
		}
	}

	public void updateAndSave(String remoteName) {
		updateAndSave(Remote.load(mContext, remoteName));
	}

	public void updateAndSave(Remote remote) {
		updateWithoutSaving(remote);
		mRemote.save(mContext);
	}

	public void updateWithoutSaving(Remote remote) {
		if (remote == null) {
			return;
		}

		mRemote = remote;
		organize();
	}

	/** Number of pixels we're away from the top */
	int mTrackHeight;

	private void organize() {

		mTrackHeight = mMarginTop;
		setupSizes();
		if ((mFlags & FLAG_COLOR) != 0)
			setupColor();

		if ((mFlags & FLAG_CORNERS) != 0)
			setupCorners();

		if ((mFlags & FLAG_ICON) != 0)
			setupIcon();
		if ((mFlags & FLAG_TEXT) != 0)
			setupText();

		if ((mFlags & FLAG_TEXT_SIZE) != 0)
			setupTextSize();

		if ((mFlags & FLAG_POSITION) != 0)
			setupPosition();
	}

	int mCurrentRowCount;

	private void setupLayout4ColsNew() {
		addRow(Button.ID_POWER, 0, Button.ID_NAV_UP, 0);
		addRow(Button.ID_VOL_UP, Button.ID_NAV_LEFT, Button.ID_NAV_OK,
				Button.ID_NAV_RIGHT);
		addRow(Button.ID_VOL_DOWN, 0, Button.ID_NAV_DOWN, 0);
		addRow(Button.ID_MUTE, Button.ID_DIGIT_1, Button.ID_DIGIT_2,
				Button.ID_DIGIT_3);
		addRow(Button.ID_CH_UP, Button.ID_DIGIT_4, Button.ID_DIGIT_5,
				Button.ID_DIGIT_6);
		addRow(Button.ID_CH_DOWN, Button.ID_DIGIT_7, Button.ID_DIGIT_8,
				Button.ID_DIGIT_9);
		addRow(Button.ID_MENU, 0, Button.ID_DIGIT_0, 0);

		int type = mRemote.options.type;
		if (type == Remote.TYPE_CABLE || type == Remote.TYPE_BLURAY) {
			addRow(Button.ID_RWD, Button.ID_PLAY, Button.ID_FFWD, Button.ID_REC);
			addRow(Button.ID_PREV, Button.ID_PAUSE, Button.ID_NEXT,
					Button.ID_STOP);
		}

		addUncommonRows();
	}

	/**
	 * Set color by ID (NOT UID)
	 */
	private void setColor(int buttonId, int color) {
		Button b = findId(buttonId);
		if (b != null)
			b.bg = color;
	}

	private void setupSizes() {
		int def = mContext.getResources().getDimensionPixelSize(
				R.dimen.default_text_size);
		def = (int) pxToDp(def);
		for (Button b : mRemote.buttons) {
			setButtonSize(b, mBlocksPerButtonX, mBlocksPerButtonY);
			b.setTextSize(def);
		}
	}

	private void setupPosition() {
		useCols(4);
		setupLayout4ColsNew();

		mRemote.buttons.addAll(mOrganizedButtons);

		mRemote.options.w = mDeviceWidth;
		mRemote.options.h = calculateHeightPx();
		mRemote.options.marginLeft = mMarginLeft;
		mRemote.options.marginTop = mMarginTop;
	}

	private void setupIcon() {
		for (Button b : mRemote.buttons) {
			b.ic = ComponentUtils.getIconIdForCommonButton(b.id);
		}
	}

	private void setupText() {
		for (Button b : mRemote.buttons) {
			b.text = ComponentUtils.getCommonButtonDisplyaName(b.id, mContext);
		}
	}

	private void setupTextSize() {
		int size = mContext.getResources().getDimensionPixelSize(
				R.dimen.default_text_size);
		size = (int) pxToDp(size);
		for (Button b : mRemote.buttons) {
			b.setTextSize(size);
		}
	}

	private void setupCorners() {
		for (Button b : mRemote.buttons) {
			setButtonCornerDp(b, 16);
		}
		setButtonCornerDp(findId(Button.ID_POWER), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_0), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_1), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_2), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_3), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_4), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_5), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_6), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_7), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_8), 400);
		setButtonCornerDp(findId(Button.ID_DIGIT_9), 400);

	}

	private void setupColor() {
		int def = Button.BG_TRANSPARENT;
		for (Button b : mRemote.buttons) {
			b.bg = def;
		}

		int vols = Button.BG_ORANGE;
		int media = Button.BG_GREY;
		int power = Button.BG_RED;
		int nav = Button.BG_BLUE_GREY;
		int numbers = Button.BG_TEAL;
		int channels = Button.BG_GREY;

		setColor(Button.ID_VOL_UP, vols);
		setColor(Button.ID_VOL_DOWN, vols);
		setColor(Button.ID_MUTE, vols);

		setColor(Button.ID_CH_UP, channels);
		setColor(Button.ID_CH_DOWN, channels);
		setColor(Button.ID_MENU, channels);

		setColor(Button.ID_POWER, power);

		setColor(Button.ID_NAV_DOWN, nav);
		setColor(Button.ID_NAV_UP, nav);
		setColor(Button.ID_NAV_LEFT, nav);
		setColor(Button.ID_NAV_RIGHT, nav);
		setColor(Button.ID_NAV_OK, nav);

		setColor(Button.ID_DIGIT_0, numbers);
		setColor(Button.ID_DIGIT_1, numbers);
		setColor(Button.ID_DIGIT_2, numbers);
		setColor(Button.ID_DIGIT_3, numbers);
		setColor(Button.ID_DIGIT_4, numbers);
		setColor(Button.ID_DIGIT_5, numbers);
		setColor(Button.ID_DIGIT_6, numbers);
		setColor(Button.ID_DIGIT_7, numbers);
		setColor(Button.ID_DIGIT_8, numbers);
		setColor(Button.ID_DIGIT_9, numbers);

		setColor(Button.ID_REC, media);
		setColor(Button.ID_STOP, media);
		setColor(Button.ID_PREV, media);
		setColor(Button.ID_NEXT, media);
		setColor(Button.ID_FFWD, media);
		setColor(Button.ID_RWD, media);
		setColor(Button.ID_PLAY, media);
		setColor(Button.ID_PAUSE, media);

	}

	private Button findId(int id) {
		return mRemote.getButtonById(id);
	}

	private int calculateHeightPx() {
		int max = 0;
		for (Button b : mRemote.buttons) {
			if (b != null)
				max = Math.max(max, (int) (b.y + b.h));
		}
		return max + mMarginTop;
	}

	private int[] getRemainingIds() {
		int[] ids = new int[mRemote.buttons.size()];
		for (int i = 0; i < mRemote.buttons.size(); i++) {
			ids[i] = mRemote.buttons.get(i).id;
		}
		return ids;
	}

	private void addUncommonRows() {
		int[] ids = getRemainingIds();
		for (int i = 0; i < ids.length; i += mCols) {
			int[] row = new int[mCols];
			for (int j = 0; j < mCols; j++) {
				if (i + j < ids.length)
					row[j] = ids[i + j];
			}
			addRow(false, row);
		}
	}

	/**
	 * Adds a row without uncommon buttons
	 */
	private void addRow(int... ids) {
		addRow(false, ids);
	}

	/**
	 * Adds a row of 4 buttons
	 * 
	 * @param buttonX
	 * @param buttonY
	 * @param includeUncommon
	 *            If set to true, this will also add buttons with uid 0, which
	 *            means that you cannot add gaps with zeros
	 * @param ids
	 */
	private void addRow(boolean includeUncommon, int... ids) {
		int y = mCurrentRowCount * mBlocksPerButtonY;
		for (int i = 0; i < Math.min(ids.length, mCols); i++) {
			if (includeUncommon || ids[i] != 0) {
				final Button b = findId(ids[i]);
				if (b != null) {
					setButtonPosition(b, 0, y, i, 0);
					moveToOrganizedList(b);
				}
			}
		}
		mCurrentRowCount++;
	}

	public void setupNewButton(Button b) {
		b.w = getButtonWidthPixels();
		b.h = getButtonHeightPixels();
		b.setCornerRadius(dpToPx(DEFAULT_CORNER_RADIUS));
		b.bg = Button.BG_GREY;
	}

	public int getButtonWidthPixels() {
		return getButtonWidthPixels(mBlocksPerButtonX);
	}

	public int getButtonHeightPixels() {
		return getButtonHeightPixels(mBlocksPerButtonY);
	}

	/**
	 * Get the width for the specified amount of blocks
	 */
	private int getButtonWidthPixels(int blocks) {
		return blocks * mGridSizeX - mGridSpacingX;
	}

	/**
	 * Get the height for the specified amount of blocks
	 */
	private int getButtonHeightPixels(int blocks) {
		return blocks * mGridSizeY - mGridSpacingY;
	}

}