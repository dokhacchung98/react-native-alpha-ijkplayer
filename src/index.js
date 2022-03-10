import React from "react";
import ReactNative, {
  requireNativeComponent, View,
  UIManager,
} from "react-native";
import PropTypes from 'prop-types'

class RNAlphaIjkplayerView extends React.Component {
  constructor(props) {
    super(props);
    this.refCurrent = React.createRef()
    this.playVideo = this.playVideo.bind(this);
  }

  setNativeProps(nativeProps) {
    this._root.setNativeProps(nativeProps);
  }

  // componentDidMount() {
    // this.playVideo();
    // this.handleReceiverProps(this.props?.isPlay, this.props?.isMuted)
  // }

  _assignRoot = (component) => {
    this._root = component;
  };

  releaseVideo() {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.releaseVideo,
        [],
      );
    } catch (er) {
      console.log('releaseVideo error', er)
    }
  }

  stopVideo() {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.stopVideo,
        [],
      );
    } catch (er) {
      console.log('stopVideo error', er)
    }
  }

  playVideo(url) {
    try {
      if (!url) return
      // this.stopVideo();
      // this.releaseVideo();
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.startVideo,
        [url],
      );
    } catch (er) {
      console.log('playVideo error', er)
    }
  }

  pauseVideo() {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.pauseVideo,
        [],
      );
    } catch (er) {
      console.log('pauseVideo error', er)
    }
  }

  resumeVideo() {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.resumeVideo,
        [],
      );
    } catch (er) {
      console.log('resumeVideo error', er)
    }
  }

  changeVolume(volume) {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.changeVolume,
        [volume],
      );
    } catch (er) {
      console.log('changeVolume error', er)
    }
  }

  seekVideo(t) {
    try {
      UIManager.dispatchViewManagerCommand(
        ReactNative.findNodeHandle(this._root),
        UIManager?.AlphaIjkplayerView?.Commands?.seekVideo,
        [t],
      );
    } catch (er) {
      console.log('seekVideo error', er)
    }
  }

  // UNSAFE_componentWillReceiveProps(nextProps) {
  //   this.handleReceiverProps(nextProps?.isPlay, nextProps?.isMuted);
  // }

  // handleReceiverProps(isP, isM) {
  //   if (isP) {
  //     this.resumeVideo();
  //   } else {
  //     this.pauseVideo();
  //   }

  //   this.changeVolume(isM ? 0 : 1);
  // }

  componentDidUpdate() {

  }

  // UNSAFE_componentWillReceiveProps(nextProps) {
  //   console.log('PROPOPS>>>', nextProps)
  // }

  componentWillUnmount() {
    this.releaseVideo();
    this.stopVideo();
  }

  render() {
    return (
      <AlphaIjkplayerView
        {...this.props}
        onVideoCompleteRN={this.props?.onVideoComplete}
        onVideoPrepareRN={this.props?.onVideoPrepare}
        onVideoErrorRN={this.props?.onVideoError}
        onVideoProgressRN={this.props?.onVideoProgress}
        onVideoInfoRN={this.props?.onVideoInfo}
        ref={this._assignRoot} />
    );
  }
}

RNAlphaIjkplayerView.propTypes = {
  ...View.propTypes,
  src: PropTypes.string,
  isMuted: PropTypes.bool,
  isPlay: PropTypes.bool,
  onVideoComplete: PropTypes.func,
  onVideoPrepare: PropTypes.func,
  onVideoError: PropTypes.func,
  onVideoProgress: PropTypes.func,
  onVideoInfo: PropTypes.func,
};

var AlphaIjkplayerView = requireNativeComponent('AlphaIjkplayerView', RNAlphaIjkplayerView, {
  nativeOnly: {
    onVideoProgressRN: true,
    onVideoPrepareRN: true,
    onVideoErrorRN: true,
    onVideoCompleteRN: true,
    onVideoInfoRN: true,
  }
});

export default RNAlphaIjkplayerView;