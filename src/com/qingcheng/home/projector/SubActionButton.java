
package com.qingcheng.home.projector;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;

import android.graphics.Path;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import java.util.ArrayList;

import com.qingcheng.home.Launcher;
import com.qingcheng.home.R;

public class SubActionButton{


    private static final int OFFSET = 72;
    Context context;

    FrameLayout frameLayout;

    View mainView;

    RotateAnimation rotateOpenAnimation;

    RotateAnimation rotateCloseAnimation;

    ArrayList<View> promotedActions;

    ObjectAnimator objectAnimator[];

    private int px;

    private static final int ANIMATION_TIME = 200;
    private int menuVideo_offset = 0;

    private boolean isMenuOpened;
    private View menuDoc;
    private View menuVideo;
    private View menuGallery;
    private View menuApps;
    private View menuSettings;
    private int[] mainCoords;
    private int[] videoCoords;
    private int[] galleryCoords;
    private int[] appsCoords;
    private int[] settingsCoords;
    private int[] docCoords;
    private int mainViewX;
    private int mainViewY;

    public void setup(Context activityContext, FrameLayout layout, View menuDoc, View menuVideo, View menuGallery, View menuApps, View menuSettings) {
        context = activityContext;
        promotedActions = new ArrayList<>();
        frameLayout = layout;
        px = (int) context.getResources().getDimension(R.dimen.dim48dp) + 10;
        openRotation();
        closeRotation();

        this.menuDoc = menuDoc;
        this.menuVideo = menuVideo;
        this.menuGallery = menuGallery;
        this.menuApps = menuApps;
        this.menuSettings = menuSettings;
    }


    public boolean isMenuOpened(){
        return isMenuOpened;
    }

    public View addMainItem(View view) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mainView = view;
        return view;
    }

    public void close(){
        if (isMenuOpened) {
            closePromotedActions().start();
            //isMenuOpened = false;
        }
    }

    public void toggle() {
        if(mainView.getAlpha() == 0){
            ((Launcher)mainView.getContext()).closeSubmenu();
            return;
        }
        if (isMenuOpened) {
            //isMenuOpened = false;
            closePromotedActions().start();
        } else {
            isMenuOpened = true;
            openPromotedActions().start();
        }
    }

    public void addItem(View subMenu, View.OnClickListener onClickListener) {

        subMenu.setOnClickListener(onClickListener);

        promotedActions.add(subMenu);

        frameLayout.addView(subMenu);

        return;
    }

    /**
     * Set close animation for promoted actions
     */
    public AnimatorSet closePromotedActions() {

        if (objectAnimator == null) {
            objectAnimatorSetup();
        }

        AnimatorSet animation = new AnimatorSet();

        for (int i = 0; i < promotedActions.size(); i++) {

            objectAnimator[i] = setCloseAnimation(promotedActions.get(i), i);
        }

        if (objectAnimator.length == 0) {
            objectAnimator = null;
        }

        hideMenu(0, 1);

        if (mainView == menuDoc) {
            animation.playTogether(objectAnimator);
            animation.setInterpolator(new FastOutLinearInInterpolator());
            animation.start();


            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
//                mainView.startAnimation(rotateCloseAnimation);
                    mainView.setClickable(false);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
//                    promotedActions.get(i).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    isMenuOpened = false;

                    mainView.setClickable(true);
                    mainView.setAlpha(1);
                    hidePromotedActions();

                    setMenuClickable(true);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    isMenuOpened = false;

                    mainView.setClickable(true);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            return animation;
        } else {
            AnimatorSet animation2 = new AnimatorSet();

            animation2.playTogether(objectAnimator[0], objectAnimator[1], objectAnimator[2], objectAnimator[3]);
            animation2.setInterpolator(new FastOutLinearInInterpolator());
            animation2.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    mainView.setClickable(false);
                    showPromotedActions();

                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    hidePromotedActions();
                    animation.playTogether(objectAnimator[4], objectAnimator[5], objectAnimator[6], objectAnimator[7], objectAnimator[8]);
                    animation.setInterpolator(new FastOutLinearInInterpolator());
                    animation.start();
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            animation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
//                mainView.startAnimation(rotateCloseAnimation);
                    mainView.setClickable(false);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
//                    promotedActions.get(i).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    isMenuOpened = false;

                    mainView.setClickable(true);
                    mainView.setAlpha(1);
                    hidePromotedActions();

                    setMenuClickable(true);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    isMenuOpened = false;

                    mainView.setClickable(true);
                    for (int i = 0; i < promotedActions.size(); i++) {
                        promotedActions.get(i).setClickable(false);
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });

            return animation2;
        }
    }

    public AnimatorSet openPromotedActions() {
        if (objectAnimator == null){
            objectAnimatorSetup();
        }

        AnimatorSet animation = new AnimatorSet();
//        AnimatorSet animationFirst = new AnimatorSet();

        for (int i = 0; i < promotedActions.size(); i++) {

            objectAnimator[i] = setOpenAnimation(promotedActions.get(i), i);
        }

        if (objectAnimator.length == 0) {
            objectAnimator = null;
        }

        showMenu(1, 0);
//        animation.playSequentially(objectAnimator[8], objectAnimator[7],objectAnimator[6],objectAnimator[5],objectAnimator[4]
//                ,objectAnimator[3], objectAnimator[2], objectAnimator[1], objectAnimator[0]);
        animation.playTogether(objectAnimator);
        animation.setInterpolator(new FastOutSlowInInterpolator());
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
//                mainView.startAnimation(rotateOpenAnimation);
                mainView.setClickable(false);
                showPromotedActions();

                for (int i = 0; i < promotedActions.size(); i++) {
                   promotedActions.get(i).setClickable(false);
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mainView.setClickable(true);
                mainView.setAlpha(1);
                for (int i = 0; i < promotedActions.size(); i++) {
                    promotedActions.get(i).setClickable(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mainView.setClickable(true);
                for (int i = 0; i < promotedActions.size(); i++) {
                    promotedActions.get(i).setClickable(true);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });


        return animation;
    }

    private void showMenu(int orgAlpha, int targetAlpha) {
        menuDoc.setAlpha(orgAlpha);
        menuDoc.setClickable(false);
        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, targetAlpha);
        ObjectAnimator docAnimator = ObjectAnimator.ofPropertyValuesHolder(menuDoc, alphA);

        menuVideo.setAlpha(orgAlpha);
        menuVideo.setClickable(false);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator vAnimator = ObjectAnimator.ofPropertyValuesHolder(menuVideo, alphA);

        menuGallery.setAlpha(orgAlpha);
        menuGallery.setClickable(false);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator gAnimator = ObjectAnimator.ofPropertyValuesHolder(menuGallery, alphA);

        menuApps.setAlpha(orgAlpha);
        menuApps.setClickable(false);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator aAnimator = ObjectAnimator.ofPropertyValuesHolder(menuApps, alphA);

        menuSettings.setAlpha(orgAlpha);
        menuSettings.setClickable(false);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator sAnimator = ObjectAnimator.ofPropertyValuesHolder(menuSettings, alphA);

        mainView.setAlpha(1);
        mainView.setClickable(false);

        if(mainView == menuDoc){
            objectAnimator[4] = sAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }else if(mainView == menuVideo){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = sAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }else if(mainView == menuGallery){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = sAnimator;
            objectAnimator[7] = aAnimator;

        }else if(mainView == menuApps){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = sAnimator;

        }else if(mainView == menuSettings){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }

        if(mainView != menuDoc){
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Path path = new Path();
                path.moveTo(mainCoords[0], mainCoords[1]);
                path.cubicTo(mainCoords[0], mainCoords[1], videoCoords[0], videoCoords[1], docCoords[0], docCoords[1]-70);
                objectAnimator[8] = ObjectAnimator.ofFloat(mainView, "x", "y", path);
            }else{
                Path path = new Path();
                path.moveTo(mainCoords[0], mainCoords[1]);
                path.cubicTo(mainCoords[0], mainCoords[1], videoCoords[0], videoCoords[1], docCoords[0]- OFFSET-20, docCoords[1]-OFFSET-20);
                objectAnimator[8] = ObjectAnimator.ofFloat(mainView, "x", "y", path);
            }

        }else{
            PropertyValuesHolder doc = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
            objectAnimator[8] = ObjectAnimator.ofPropertyValuesHolder(mainView, doc);
        }
//        objectAnimator[8] = orgAlpha == 1 ? setOpenAnimation(mainView, 4) : setCloseAnimation(mainView, 4);
    }

    private void setMenuClickable(boolean clickable){
        menuDoc.setClickable(clickable);
        menuVideo.setClickable(clickable);
        menuGallery.setClickable(clickable);
        menuApps.setClickable(clickable);
        menuSettings.setClickable(clickable);
    }

    private void hideMenu(int orgAlpha, int targetAlpha) {
        menuDoc.setAlpha(orgAlpha);
        //menuDoc.setClickable(true);
        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, targetAlpha);
        ObjectAnimator docAnimator = ObjectAnimator.ofPropertyValuesHolder(menuDoc, alphA);

        menuVideo.setAlpha(orgAlpha);
        //menuVideo.setClickable(true);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator vAnimator = ObjectAnimator.ofPropertyValuesHolder(menuVideo, alphA);

        menuGallery.setAlpha(orgAlpha);
        //menuGallery.setClickable(true);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator gAnimator = ObjectAnimator.ofPropertyValuesHolder(menuGallery, alphA);

        menuApps.setAlpha(orgAlpha);
        //menuApps.setClickable(true);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator aAnimator = ObjectAnimator.ofPropertyValuesHolder(menuApps, alphA);

        menuSettings.setAlpha(orgAlpha);
        //menuSettings.setClickable(true);
//        PropertyValuesHolder alphA = PropertyValuesHolder.ofFloat(View.ALPHA, 0 );
        ObjectAnimator sAnimator = ObjectAnimator.ofPropertyValuesHolder(menuSettings, alphA);

        mainView.setAlpha(1);

        if(mainView == menuDoc){
            objectAnimator[4] = sAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }else if(mainView == menuVideo){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = sAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }else if(mainView == menuGallery){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = sAnimator;
            objectAnimator[7] = aAnimator;

        }else if(mainView == menuApps){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = sAnimator;

        }else if(mainView == menuSettings){
            objectAnimator[4] = docAnimator;
            objectAnimator[5] = vAnimator;
            objectAnimator[6] = gAnimator;
            objectAnimator[7] = aAnimator;
        }
        if(mainView != menuDoc){
            if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Path path = new Path();
                path.moveTo(docCoords[0] - 10, docCoords[1]-70);
                path.cubicTo(docCoords[0] - 10 , docCoords[1]-70, videoCoords[0], videoCoords[1], mainCoords[0]-20, mainCoords[1] -OFFSET);
                objectAnimator[8] = ObjectAnimator.ofFloat(mainView, "x", "y", path);
            }else{
                Path path = new Path();
                path.moveTo(docCoords[0], docCoords[1]-100);
                path.cubicTo(docCoords[0], docCoords[1]-100, videoCoords[0], videoCoords[1], mainCoords[0]-OFFSET, mainCoords[1]-OFFSET);
                objectAnimator[8] = ObjectAnimator.ofFloat(mainView, "x", "y", path);
            }
        }else{
            PropertyValuesHolder doc = PropertyValuesHolder.ofFloat(View.ALPHA, 1);
            objectAnimator[8] = ObjectAnimator.ofPropertyValuesHolder(mainView, doc);
        }
//        objectAnimator[8] = orgAlpha == 1 ? setOpenAnimation(mainView, 4) : setCloseAnimation(mainView, 4);
    }

    private void objectAnimatorSetup() {

        objectAnimator = new ObjectAnimator[promotedActions.size() + 5];
    }


    /**
     * Set close animation for single button
     *
     * @param promotedAction
     * @param position
     * @return objectAnimator
     */
    private ObjectAnimator setCloseAnimation(View promotedAction, int position) {

        ObjectAnimator objectAnimator;

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            int[] orgCoords = new int[2];
            mainView.getLocationOnScreen(orgCoords);
            float orgX = orgCoords[0];
            float orgY = orgCoords[1];

            if(mainView == menuVideo){
//                orgY = orgY - 20;
                menuVideo_offset = 75;
            }else if(mainView == menuDoc){
                menuVideo_offset = 10;
                orgX = orgX-10;
            }else if(mainView == menuGallery){
                menuVideo_offset = 10;
                orgX = orgX-10;
            }

            Path path = new Path();

            int[] targetCoords = new int[2];
            switch (position){
                case 0:
                    menuVideo.getLocationOnScreen(targetCoords);
//                    targetX = menuVideo.getTranslationX();
//                    targetY = menuVideo.getTranslationY();
                    if (mainView != menuVideo) {
                        path.moveTo(targetCoords[0], targetCoords[1] - menuVideo_offset);
                    } else {
                        path.moveTo(videoCoords[0], videoCoords[1] - menuVideo_offset);
                    }
                    path.cubicTo(videoCoords[0], videoCoords[1] - menuVideo_offset, videoCoords[0] - 20, videoCoords[1] - 20, orgX, orgY - menuVideo_offset);

                    promotedAction.setTranslationY(videoCoords[1]);
                    promotedAction.setTranslationX(videoCoords[0]);

                    break;
                case 1:
                    menuGallery.getLocationOnScreen(targetCoords);
                    if (mainView != menuGallery) {
                        path.moveTo(targetCoords[0], targetCoords[1] - menuVideo_offset);
                    } else {
                        path.moveTo(galleryCoords[0], galleryCoords[1] - menuVideo_offset);
                    }
                    path.cubicTo(galleryCoords[0], galleryCoords[1] - menuVideo_offset, videoCoords[0], videoCoords[1], orgX, orgY - menuVideo_offset);

                    promotedAction.setTranslationY(galleryCoords[1]);
                    promotedAction.setTranslationX(galleryCoords[0]);

                    break;
                case 2:
                    menuApps.getLocationOnScreen(targetCoords);
//                    targetX = menuApps.getTranslationX();
//                    targetY = menuApps.getTranslationY();
                    path.moveTo(targetCoords[0], targetCoords[1] - menuVideo_offset);
                    path.cubicTo(appsCoords[0], appsCoords[1], galleryCoords[0] - menuVideo_offset, galleryCoords[1], orgX, orgY - menuVideo_offset);
                    promotedAction.setTranslationY(appsCoords[1]);
                    promotedAction.setTranslationX(appsCoords[0]);
                    break;
                case 3:
                    menuSettings.getLocationOnScreen(targetCoords);
//                    targetX = menuSettings.getTranslationX();
//                    targetY = menuSettings.getTranslationY();
                    path.moveTo(settingsCoords[0], settingsCoords[1]);
                    path.cubicTo(settingsCoords[0], settingsCoords[1], galleryCoords[0], galleryCoords[1], orgX, orgY);
                    promotedAction.setTranslationY(settingsCoords[1]);
                    promotedAction.setTranslationX(settingsCoords[0]);
                    break;
                case 4:
                    promotedAction.getLocationOnScreen(targetCoords);
                    path.moveTo(targetCoords[0], targetCoords[1]);
                    path.cubicTo(targetCoords[0], targetCoords[1], videoCoords[0], videoCoords[1], mainViewX, mainViewY);
                    promotedAction.setTranslationY(targetCoords[1]);
                    promotedAction.setTranslationX(targetCoords[0]);
                    break;
            }


//            float targetX = targetCoords[0];
//            float targetY = targetCoords[1];

            objectAnimator = ObjectAnimator.ofFloat(promotedAction, "x", "y", path);


            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.setDuration(ANIMATION_TIME/* * (promotedActions.size() - position)*/);

        } else {
            int[] orgCoords = new int[2];
            mainView.getLocationOnScreen(orgCoords);
            float orgX = orgCoords[0] - OFFSET;
            float orgY = orgCoords[1] - OFFSET;

            Path path = new Path();

            int[] targetCoords = new int[2];
            switch (position){
                case 0:
                    menuVideo.getLocationOnScreen(targetCoords);
                    path.moveTo(targetCoords[0], targetCoords[1]);
                    path.cubicTo(videoCoords[0], videoCoords[1], videoCoords[0] - 20, videoCoords[1] - 20,orgX, orgY);
                    promotedAction.setTranslationY(videoCoords[1]);
                    promotedAction.setTranslationX(videoCoords[1]);
                    break;
                case 1:
                    menuGallery.getLocationOnScreen(targetCoords);
                    path.moveTo(targetCoords[0], targetCoords[1]);
                    path.cubicTo(galleryCoords[0], galleryCoords[1], videoCoords[0], videoCoords[1], orgX, orgY);
                    promotedAction.setTranslationY(galleryCoords[1]);
                    promotedAction.setTranslationX(galleryCoords[0]);
                    break;
                case 2:
                    menuApps.getLocationOnScreen(targetCoords);
                    path.moveTo(targetCoords[0], targetCoords[1]);
                    path.cubicTo(appsCoords[0], appsCoords[1], galleryCoords[0], galleryCoords[1], orgX, orgY);
                    promotedAction.setTranslationY(appsCoords[1]);
                    promotedAction.setTranslationX(appsCoords[0]);
                    break;
                case 3:
                    menuSettings.getLocationOnScreen(targetCoords);
                    path.moveTo(settingsCoords[0], settingsCoords[1]);
                    path.cubicTo(settingsCoords[0], settingsCoords[1], galleryCoords[0], galleryCoords[1], orgX, orgY);
                    promotedAction.setTranslationY(settingsCoords[1]);
                    promotedAction.setTranslationX(settingsCoords[0]);
                    break;
            }

            objectAnimator = ObjectAnimator.ofFloat(promotedAction, "x", "y", path);

            objectAnimator.setInterpolator(new AccelerateInterpolator());
            objectAnimator.setDuration(ANIMATION_TIME/* * (promotedActions.size() - position)*/);

        }

        return objectAnimator;
    }

    /**
     * Set open animation for single button
     *
     * @param promotedAction
     * @param position
     * @return objectAnimator
     */
    private ObjectAnimator setOpenAnimation(View promotedAction, int position) {
        ObjectAnimator objectAnimator;

        if (videoCoords == null) {
            videoCoords = new int[2];
            menuVideo.getLocationOnScreen(videoCoords);
        }

        if (galleryCoords == null) {
            galleryCoords = new int[2];
            menuGallery.getLocationOnScreen(galleryCoords);
        }

        if (appsCoords == null) {
            appsCoords = new int[2];
            menuApps.getLocationOnScreen(appsCoords);
        }

        if (settingsCoords == null) {
            settingsCoords = new int[2];
            menuSettings.getLocationOnScreen(settingsCoords);
        }

        if (docCoords == null) {
            docCoords = new int[2];
            menuDoc.getLocationOnScreen(docCoords);
        }

        if (mainCoords == null) {
            mainCoords = new int[2];
            mainView.getLocationOnScreen(mainCoords);
            mainViewX = mainCoords[0];
            mainViewY = mainCoords[1];
        }

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            int[] orgCoords = new int[2];
            if(mainView == menuDoc){
                mainView.getLocationOnScreen(orgCoords);
            }
            else{
                menuDoc.getLocationOnScreen(orgCoords);
            }
            float orgX = orgCoords[0];
            float orgY = orgCoords[1];

            if(mainView == menuVideo){
                orgY = orgY - 60;
                menuVideo_offset = 60;
            }
            Path path = new Path();
            path.moveTo(orgX, orgY);
            int[] targetCoords = new int[2];
            switch (position){
                case 0:
                    menuVideo.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, orgX + 20, orgY - menuVideo_offset, targetCoords[0], targetCoords[1] - menuVideo_offset);
                    promotedAction.setTranslationY(orgY);
                    promotedAction.setTranslationX(orgX);
                    break;
                case 1:
                    menuGallery.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, videoCoords[0], videoCoords[1] - menuVideo_offset, targetCoords[0], targetCoords[1] - menuVideo_offset);
                    promotedAction.setTranslationY(orgY);
                    promotedAction.setTranslationX(orgX);
                    break;
                case 2:
                    menuApps.getLocationOnScreen(targetCoords);;
                    path.cubicTo(orgX, orgY, galleryCoords[0], galleryCoords[1] - menuVideo_offset, targetCoords[0], targetCoords[1] - menuVideo_offset);
                    promotedAction.setTranslationY(orgY);
                    promotedAction.setTranslationX(orgX);
                    break;
                case 3:
                    menuSettings.getLocationOnScreen(targetCoords);
                    path.cubicTo(videoCoords[0], videoCoords[1], galleryCoords[0], galleryCoords[1], targetCoords[0], targetCoords[1]);
                    promotedAction.setTranslationY(orgY);
                    promotedAction.setTranslationX(orgX);
                    break;

                case 4:
                    path.cubicTo(mainCoords[0], mainCoords[1], videoCoords[0], videoCoords[1], docCoords[0], docCoords[1]);
                    promotedAction.setTranslationY(mainCoords[1]);
                    promotedAction.setTranslationX(mainCoords[0]);
                    break;
            }
            objectAnimator = ObjectAnimator.ofFloat(promotedAction, "x", "y", path);
            objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            objectAnimator.setDuration(ANIMATION_TIME * (promotedActions.size() - position));

        } else {
            int[] orgCoords = new int[2];
            mainView.getLocationOnScreen(orgCoords);
            float orgX = orgCoords[0] - OFFSET;
            float orgY = orgCoords[1] - OFFSET;

            Path path = new Path();
            path.moveTo(orgX, orgY);
            int[] targetCoords = new int[2];
            switch (position){
                case 0:
                    menuVideo.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, orgX - OFFSET, orgY-OFFSET, targetCoords[0] - OFFSET, targetCoords[1] - OFFSET);
                    break;
                case 1:
                    menuGallery.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, videoCoords[0], videoCoords[1], targetCoords[0] - OFFSET, targetCoords[1] - OFFSET);
                    break;
                case 2:
                    menuApps.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, galleryCoords[0], galleryCoords[1], targetCoords[0] - OFFSET, targetCoords[1] - OFFSET);
                    break;
                case 3:
                    menuSettings.getLocationOnScreen(targetCoords);
                    path.cubicTo(orgX, orgY, galleryCoords[0], galleryCoords[1], targetCoords[0] - OFFSET, targetCoords[1] - OFFSET);
                    break;
            }

            promotedAction.setTranslationY(orgY);
            promotedAction.setTranslationX(orgX);

            objectAnimator = ObjectAnimator.ofFloat(promotedAction, "x", "y", path);


            objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            objectAnimator.setDuration(ANIMATION_TIME * (promotedActions.size() - position));
        }

        return objectAnimator;
    }

    private void hidePromotedActions() {

        for (int i = 0; i < promotedActions.size(); i++) {
            promotedActions.get(i).setVisibility(View.GONE);
        }
    }

    private void showPromotedActions() {

        for (int i = 0; i < promotedActions.size(); i++) {
            promotedActions.get(i).setVisibility(View.VISIBLE);
        }
    }

    public ArrayList<View> getViewItems(){
        return promotedActions;
    }

    private void openRotation() {
        rotateOpenAnimation = new RotateAnimation(0, 45, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        rotateOpenAnimation.setFillAfter(true);
        rotateOpenAnimation.setFillEnabled(true);
        rotateOpenAnimation.setDuration(ANIMATION_TIME);
    }

    private void closeRotation() {
        rotateCloseAnimation = new RotateAnimation(45, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateCloseAnimation.setFillAfter(true);
        rotateCloseAnimation.setFillEnabled(true);
        rotateCloseAnimation.setDuration(ANIMATION_TIME);
    }
}
